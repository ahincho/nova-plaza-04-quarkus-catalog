package pe.edu.nova.plaza.catalog.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.api.standard.error.DomainError;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;
import pe.edu.nova.plaza.catalog.inventory.domain.Product;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationLine;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationStatus;
import pe.edu.nova.plaza.catalog.inventory.exception.CatalogErrors;
import pe.edu.nova.plaza.catalog.inventory.port.in.ProductView;
import pe.edu.nova.plaza.catalog.inventory.port.in.StockReservations.Item;

class StockReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T15:00:00.123456789Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final InMemoryStores.Reservations reservations = new InMemoryStores.Reservations();
    private final InMemoryStores.Products products = new InMemoryStores.Products(
            reservations,
            new Product("MUG-001", "Taza", new BigDecimal("25.50"), "PEN", 10),
            new Product("TEE-002", "Polo", new BigDecimal("49.90"), "PEN", 5),
            new Product("USD-100", "Importado", new BigDecimal("10.00"), "USD", 5));
    private final InMemoryStores.MutableClock clock = new InMemoryStores.MutableClock(NOW);
    private final StockReservationService service =
            new StockReservationService(products, reservations, clock, () -> TTL);
    private final ProductCatalogService catalog = new ProductCatalogService(products, clock);

    @Test
    void aReservationTakesTheCatalogPricesAndHoldsTheStockForTenMinutes() {
        Reservation reservation = service.reserve("customer-1", null, List.of(new Item("MUG-001", 2), new Item("TEE-002", 1)));

        assertEquals(ReservationStatus.HELD, reservation.status());
        assertEquals("PEN", reservation.currency());
        assertEquals(
                List.of(
                        new ReservationLine("MUG-001", 2, new BigDecimal("25.50")),
                        new ReservationLine("TEE-002", 1, new BigDecimal("49.90"))),
                reservation.lines());
        assertEquals(new BigDecimal("100.90"), reservation.total());
        assertEquals(Instant.parse("2026-10-02T15:10:00.123456Z"), reservation.expiresAt());
        assertEquals(8, catalog.find("MUG-001").available());
        assertEquals(10, products.rows.get("MUG-001").stock());
    }

    @Test
    void aRepeatedProductIsOneLineWithTheSumOfItsQuantities() {
        Reservation reservation = service.reserve("customer-1", null, List.of(new Item("MUG-001", 2), new Item("MUG-001", 3)));

        assertEquals(List.of(new ReservationLine("MUG-001", 5, new BigDecimal("25.50"))), reservation.lines());
    }

    @Test
    void theSamePurchaseRepeatedGetsTheSameReservationWithoutHoldingMore() {
        Reservation first = service.reserve("customer-1", "purchase-1", List.of(new Item("TEE-002", 3)));
        service.confirm(first.id());

        Reservation again = service.reserve("customer-1", "purchase-1", List.of(new Item("TEE-002", 3)));

        assertEquals(first.id(), again.id());
        assertEquals(ReservationStatus.CONFIRMED, again.status());
        assertEquals(1, reservations.rows.size());
        assertEquals(2, products.rows.get("TEE-002").stock());
    }

    @Test
    void theSameKeyWithOtherProductsIsRejectedAndAnotherCustomersKeyIsAnotherPurchase() {
        service.reserve("customer-1", "purchase-1", List.of(new Item("MUG-001", 1)));

        pe.edu.nova.java.libs.api.standard.error.ApplicationError reused = assertThrows(
                pe.edu.nova.java.libs.api.standard.error.ApplicationError.class,
                () -> service.reserve("customer-1", "purchase-1", List.of(new Item("MUG-001", 2))));
        Reservation theirs = service.reserve("customer-2", "purchase-1", List.of(new Item("MUG-001", 2)));

        assertEquals(CatalogErrors.IDEMPOTENCY_KEY_REUSED, reused.code().orElseThrow());
        assertEquals("customer-2", theirs.customerId());
        assertEquals(2, reservations.rows.size());
    }

    @Test
    void reservingMoreThanWhatIsAvailableIsOutOfStock() {
        service.reserve("customer-1", null, List.of(new Item("TEE-002", 4)));

        DomainError error =
                assertThrows(DomainError.class, () -> service.reserve("customer-2", null, List.of(new Item("TEE-002", 2))));

        assertEquals(DomainError.Type.CONFLICT, error.type());
        assertEquals(CatalogErrors.OUT_OF_STOCK, error.code().orElseThrow());
        assertEquals(1, reservations.rows.size());
    }

    @Test
    void anUnknownProductOrMixedCurrenciesAreRejected() {
        DomainError missing =
                assertThrows(DomainError.class, () -> service.reserve("customer-1", null, List.of(new Item("NOPE-000", 1))));
        DomainError mixed = assertThrows(
                DomainError.class,
                () -> service.reserve("customer-1", null, List.of(new Item("MUG-001", 1), new Item("USD-100", 1))));

        assertEquals(CatalogErrors.PRODUCT_NOT_FOUND, missing.code().orElseThrow());
        assertEquals(DomainError.Type.RULE_VIOLATION, mixed.type());
        assertEquals(CatalogErrors.MIXED_CURRENCIES, mixed.code().orElseThrow());
    }

    @Test
    void confirmingTakesTheStockOutAndRepeatingItChangesNothing() {
        Reservation reservation = service.reserve("customer-1", null, List.of(new Item("MUG-001", 3)));

        Reservation confirmed = service.confirm(reservation.id());
        Reservation again = service.confirm(reservation.id());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.status());
        assertEquals(confirmed, again);
        assertEquals(7, products.rows.get("MUG-001").stock());
        assertEquals(7, catalog.find("MUG-001").available());
    }

    @Test
    void anExpiredReservationNoLongerHoldsStockAndCannotBeConfirmed() {
        Reservation reservation = service.reserve("customer-1", null, List.of(new Item("TEE-002", 5)));
        clock.advance(TTL);

        assertEquals(5, catalog.find("TEE-002").available());
        DomainError error = assertThrows(DomainError.class, () -> service.confirm(reservation.id()));
        assertEquals(CatalogErrors.RESERVATION_EXPIRED, error.code().orElseThrow());
        assertEquals(5, products.rows.get("TEE-002").stock());
    }

    @Test
    void releasingGivesTheStockBackAndRepeatingItChangesNothing() {
        Reservation reservation = service.reserve("customer-1", null, List.of(new Item("TEE-002", 5)));

        Reservation released = service.release(reservation.id());

        assertEquals(ReservationStatus.RELEASED, released.status());
        assertEquals(released, service.release(reservation.id()));
        assertEquals(5, catalog.find("TEE-002").available());
        DomainError error = assertThrows(DomainError.class, () -> service.confirm(reservation.id()));
        assertEquals(CatalogErrors.RESERVATION_RELEASED, error.code().orElseThrow());
    }

    @Test
    void aConfirmedReservationCannotBeReleased() {
        Reservation reservation = service.reserve("customer-1", null, List.of(new Item("MUG-001", 1)));
        service.confirm(reservation.id());

        DomainError error = assertThrows(DomainError.class, () -> service.release(reservation.id()));

        assertEquals(DomainError.Type.CONFLICT, error.type());
        assertEquals(CatalogErrors.RESERVATION_CONFIRMED, error.code().orElseThrow());
    }

    @Test
    void anUnknownReservationIsNotFound() {
        UUID id = UUID.randomUUID();

        assertSame(DomainError.Type.NOT_FOUND, assertThrows(DomainError.class, () -> service.confirm(id)).type());
        assertSame(DomainError.Type.NOT_FOUND, assertThrows(DomainError.class, () -> service.release(id)).type());
    }

    @Test
    void theCatalogScrollsByCodeWithWhatIsAvailable() {
        service.reserve("customer-1", null, List.of(new Item("MUG-001", 4)));

        CursorPage<ProductView> first = catalog.page(CursorRequest.first(2));
        CursorPage<ProductView> second = catalog.page(CursorRequest.after(first.nextCursor(), 2));

        assertEquals(List.of("MUG-001", "TEE-002"), first.items().stream().map(ProductView::sku).toList());
        assertEquals(6, first.items().get(0).available());
        assertEquals(List.of("USD-100"), second.items().stream().map(ProductView::sku).toList());
        assertEquals(false, second.hasNext());
        assertEquals(
                CatalogErrors.PRODUCT_NOT_FOUND,
                assertThrows(DomainError.class, () -> catalog.find("NOPE-000")).code().orElseThrow());
    }
}
