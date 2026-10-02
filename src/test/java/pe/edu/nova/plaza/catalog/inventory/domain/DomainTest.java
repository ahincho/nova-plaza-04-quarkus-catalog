package pe.edu.nova.plaza.catalog.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainTest {

    private static final Instant NOW = Instant.parse("2026-10-02T15:00:00Z");

    @Test
    void whatIsAvailableIsTheStockMinusWhatIsHeldAndNeverNegative() {
        Product mug = new Product("MUG-001", "Taza", new BigDecimal("25.50"), "PEN", 10);

        assertEquals(10, mug.available(0));
        assertEquals(3, mug.available(7));
        assertEquals(0, mug.available(12));
        assertEquals(6, mug.withdraw(4).stock());
        assertThrows(IllegalArgumentException.class, () -> mug.withdraw(11));
    }

    @Test
    void aHeldReservationExpiresAtItsDeadlineButAConfirmedOneNever() {
        Reservation held = Reservation.hold(
                "customer-1",
                "PEN",
                List.of(new ReservationLine("MUG-001", 2, new BigDecimal("25.50"))),
                NOW,
                Duration.ofMinutes(10));

        assertFalse(held.isExpiredAt(NOW.plusSeconds(599)));
        assertTrue(held.isExpiredAt(NOW.plusSeconds(600)));
        assertFalse(held.confirmed().isExpiredAt(NOW.plusSeconds(600)));
        assertEquals(ReservationStatus.RELEASED, held.released().status());
        assertEquals(new BigDecimal("51.00"), held.total());
    }

    @Test
    void aLineNeedsAPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new ReservationLine("MUG-001", 0, BigDecimal.ONE));
    }
}
