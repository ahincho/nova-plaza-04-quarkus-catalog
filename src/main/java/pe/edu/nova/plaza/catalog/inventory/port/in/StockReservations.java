package pe.edu.nova.plaza.catalog.inventory.port.in;

import java.util.List;
import java.util.UUID;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;

/** Los casos de uso de las reservas de stock (ADR-043 y ADR-056). */
public interface StockReservations {

    /**
     * Aparta el stock de una compra y fija los precios del momento. Con la clave de la compra, repetirla con los
     * mismos productos devuelve la misma reserva, y con otros es un 422 {@code IDEMPOTENCY_KEY_REUSED}.
     *
     * @param customerId     el cliente que compra
     * @param idempotencyKey la clave de la compra, o {@code null}
     * @param items          los productos y sus cantidades; un producto repetido suma sus cantidades
     * @return la reserva
     */
    Reservation reserve(String customerId, String idempotencyKey, List<Item> items);

    /**
     * Confirma una reserva: el stock sale del almacén. Repetirla devuelve la misma reserva.
     *
     * @param id la reserva
     * @return la reserva confirmada
     */
    Reservation confirm(UUID id);

    /**
     * Libera una reserva: el stock vuelve a estar disponible. Repetirla devuelve la misma reserva.
     *
     * @param id la reserva
     * @return la reserva liberada
     */
    Reservation release(UUID id);

    /**
     * Un producto y las unidades que se quieren apartar.
     *
     * @param sku      el código del producto
     * @param quantity las unidades
     */
    record Item(String sku, int quantity) {}
}
