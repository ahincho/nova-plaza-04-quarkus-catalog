package pe.edu.nova.plaza.catalog.inventory.service;

import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import pe.edu.nova.plaza.catalog.inventory.domain.Product;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationLine;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationStatus;
import pe.edu.nova.plaza.catalog.inventory.exception.CatalogErrors;
import pe.edu.nova.plaza.catalog.inventory.port.in.StockReservations;
import pe.edu.nova.plaza.catalog.inventory.port.out.ProductStore;
import pe.edu.nova.plaza.catalog.inventory.port.out.ReservationStore;

/**
 * Aparta, confirma y libera el stock de una compra (ADR-043 y ADR-056).
 *
 * <p>Cada operación corre en una transacción y toma los productos que toca, ordenados por código: dos
 * compras del último stock se atienden una detrás de otra, y la segunda recibe el 409.
 */
@ApplicationScoped
public class StockReservationService implements StockReservations {

    private final ProductStore products;
    private final ReservationStore reservations;
    private final Clock clock;
    private final Duration ttl;

    /**
     * Crea el caso de uso.
     *
     * @param products     los productos guardados
     * @param reservations las reservas guardadas
     * @param clock        el reloj del servicio
     * @param settings     cuánto aparta el stock una reserva
     */
    public StockReservationService(
            ProductStore products, ReservationStore reservations, Clock clock, Settings settings) {
        this.products = products;
        this.reservations = reservations;
        this.clock = clock;
        this.ttl = settings.reservationTtl();
    }

    @Override
    @Transactional
    public Reservation reserve(String customerId, List<Item> items) {
        // Un producto repetido es una sola línea, con la suma de sus cantidades.
        Map<String, Integer> requested =
                items.stream().collect(Collectors.toMap(Item::sku, Item::quantity, Integer::sum, TreeMap::new));
        Map<String, Product> locked =
                products.lock(requested.keySet()).stream().collect(Collectors.toMap(Product::sku, Function.identity()));
        Instant now = clock.instant();
        Map<String, Integer> held = products.held(requested.keySet(), now);

        List<ReservationLine> lines = requested.entrySet().stream()
                .map(entry -> line(locked, held, entry.getKey(), entry.getValue()))
                .toList();
        List<String> currencies =
                lines.stream().map(line -> locked.get(line.sku()).currency()).distinct().toList();
        if (currencies.size() > 1) {
            throw CatalogErrors.mixedCurrencies();
        }

        Reservation reservation = Reservation.hold(customerId, currencies.get(0), lines, now, ttl);
        reservations.save(reservation);
        return reservation;
    }

    @Override
    @Transactional
    public Reservation confirm(UUID id) {
        Reservation reservation = reservations.lock(id).orElseThrow(() -> CatalogErrors.reservationNotFound(id));
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            return reservation;
        }
        if (reservation.status() == ReservationStatus.RELEASED) {
            throw CatalogErrors.reservationReleased(id);
        }
        if (reservation.isExpiredAt(clock.instant())) {
            throw CatalogErrors.reservationExpired(id);
        }

        Map<String, Integer> quantities = reservation.lines().stream()
                .collect(Collectors.toMap(ReservationLine::sku, ReservationLine::quantity));
        products.lock(quantities.keySet())
                .forEach(product -> products.save(product.withdraw(quantities.get(product.sku()))));
        Reservation confirmed = reservation.confirmed();
        reservations.save(confirmed);
        return confirmed;
    }

    @Override
    @Transactional
    public Reservation release(UUID id) {
        Reservation reservation = reservations.lock(id).orElseThrow(() -> CatalogErrors.reservationNotFound(id));
        if (reservation.status() == ReservationStatus.RELEASED) {
            return reservation;
        }
        if (reservation.status() == ReservationStatus.CONFIRMED) {
            throw CatalogErrors.reservationConfirmed(id);
        }
        Reservation released = reservation.released();
        reservations.save(released);
        return released;
    }

    private static ReservationLine line(
            Map<String, Product> locked, Map<String, Integer> held, String sku, int quantity) {
        Product product = locked.get(sku);
        if (product == null) {
            throw CatalogErrors.productNotFound(sku);
        }
        int available = product.available(held.getOrDefault(sku, 0));
        if (quantity > available) {
            throw CatalogErrors.outOfStock(sku, quantity, available);
        }
        return new ReservationLine(sku, quantity, product.price());
    }

    /** La configuración de las reservas, bajo {@code plaza.catalog}. */
    @ConfigMapping(prefix = "plaza.catalog")
    public interface Settings {

        /**
         * Cuánto aparta el stock una reserva antes de vencer.
         *
         * @return diez minutos, por ADR-043
         */
        Duration reservationTtl();
    }
}
