package pe.edu.nova.plaza.catalog.inventory.port.out;

import java.util.Optional;
import java.util.UUID;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;

/** Las reservas guardadas. */
public interface ReservationStore {

    /**
     * Toma una reserva para cambiar su estado: dos confirmaciones simultáneas no descuentan dos veces.
     *
     * @param id la reserva
     * @return la reserva, si existe
     */
    Optional<Reservation> lock(UUID id);

    /**
     * La reserva que pidió una compra, por su clave: repetir la compra la encuentra.
     *
     * @param customerId     el cliente
     * @param idempotencyKey la clave de la compra
     * @return la reserva, si esa compra ya reservó
     */
    Optional<Reservation> findByKey(String customerId, String idempotencyKey);

    /**
     * Guarda una reserva nueva o su estado nuevo.
     *
     * @param reservation la reserva
     */
    void save(Reservation reservation);
}
