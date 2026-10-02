package pe.edu.nova.plaza.catalog.inventory.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * El stock apartado para una compra que todavía no terminó (ADR-043).
 *
 * <p>Nace {@link ReservationStatus#HELD} y vence sola: pasado {@code expiresAt}, ya no aparta stock aunque
 * nadie la haya liberado. Así una compensación que se pierde no deja stock bloqueado. Las transiciones no
 * deciden si están permitidas; eso lo dice el caso de uso, que responde con el error que corresponde.
 *
 * @param id         el identificador
 * @param customerId el cliente que compra
 * @param status     el estado
 * @param currency   la moneda de todas las líneas
 * @param createdAt  cuándo se reservó
 * @param expiresAt  cuándo deja de apartar stock si sigue {@code HELD}
 * @param lines      las líneas, con el precio del momento
 */
public record Reservation(
        UUID id,
        String customerId,
        ReservationStatus status,
        String currency,
        Instant createdAt,
        Instant expiresAt,
        List<ReservationLine> lines) {

    /** Crea la reserva. */
    public Reservation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        lines = List.copyOf(lines);
    }

    /**
     * Aparta stock para una compra.
     *
     * @param customerId el cliente
     * @param currency   la moneda de las líneas
     * @param lines      las líneas
     * @param now        el momento de la reserva
     * @param ttl        cuánto aparta el stock
     * @return la reserva, todavía sin guardar
     */
    public static Reservation hold(
            String customerId, String currency, List<ReservationLine> lines, Instant now, Duration ttl) {
        // Postgres guarda microsegundos: con la misma precisión, lo que se lee es lo que se escribió.
        Instant createdAt = now.truncatedTo(ChronoUnit.MICROS);
        return new Reservation(
                UUID.randomUUID(), customerId, ReservationStatus.HELD, currency, createdAt, createdAt.plus(ttl), lines);
    }

    /**
     * Si la reserva ya no aparta stock porque pasó su plazo sin confirmarse ni liberarse.
     *
     * @param now el momento de la consulta
     * @return {@code true} si sigue {@code HELD} y ya venció
     */
    public boolean isExpiredAt(Instant now) {
        return status == ReservationStatus.HELD && !now.isBefore(expiresAt);
    }

    /**
     * La reserva confirmada.
     *
     * @return la misma reserva, {@code CONFIRMED}
     */
    public Reservation confirmed() {
        return withStatus(ReservationStatus.CONFIRMED);
    }

    /**
     * La reserva liberada.
     *
     * @return la misma reserva, {@code RELEASED}
     */
    public Reservation released() {
        return withStatus(ReservationStatus.RELEASED);
    }

    /**
     * El total de la reserva.
     *
     * @return la suma de las líneas
     */
    public BigDecimal total() {
        return lines.stream().map(ReservationLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Reservation withStatus(ReservationStatus next) {
        return new Reservation(id, customerId, next, currency, createdAt, expiresAt, lines);
    }
}
