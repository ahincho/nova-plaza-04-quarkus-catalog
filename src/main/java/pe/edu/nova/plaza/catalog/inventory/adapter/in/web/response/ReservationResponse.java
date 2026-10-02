package pe.edu.nova.plaza.catalog.inventory.adapter.in.web.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;

/**
 * Una reserva en JSON. Sus líneas tienen la forma que pedidos espera al crear un pedido, así que el BFF las pasa
 * sin reescribirlas.
 *
 * @param id        la reserva
 * @param status    {@code HELD}, {@code CONFIRMED} o {@code RELEASED}
 * @param currency  la moneda
 * @param total     la suma de las líneas
 * @param expiresAt cuándo deja de apartar stock si no se confirma
 * @param items     las líneas, con el precio del momento
 */
public record ReservationResponse(
        UUID id, String status, String currency, BigDecimal total, Instant expiresAt, List<Item> items) {

    /**
     * Convierte la reserva.
     *
     * @param reservation la reserva
     * @return la respuesta
     */
    public static ReservationResponse of(Reservation reservation) {
        List<Item> items = reservation.lines().stream()
                .map(line -> new Item(line.sku(), line.quantity(), line.unitPrice()))
                .toList();
        return new ReservationResponse(
                reservation.id(),
                reservation.status().name(),
                reservation.currency(),
                reservation.total(),
                reservation.expiresAt(),
                items);
    }

    /**
     * Una línea.
     *
     * @param sku       el código del producto
     * @param quantity  las unidades
     * @param unitPrice el precio de una unidad al reservar
     */
    public record Item(String sku, int quantity, BigDecimal unitPrice) {}
}
