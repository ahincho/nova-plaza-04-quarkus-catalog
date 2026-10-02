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
     * Guarda una reserva nueva o su estado nuevo.
     *
     * @param reservation la reserva
     */
    void save(Reservation reservation);
}
