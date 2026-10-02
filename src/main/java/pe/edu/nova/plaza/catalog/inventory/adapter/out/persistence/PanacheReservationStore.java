package pe.edu.nova.plaza.catalog.inventory.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;
import pe.edu.nova.plaza.catalog.inventory.port.out.ReservationStore;

/** Las reservas sobre Postgres, con Panache. */
@ApplicationScoped
public class PanacheReservationStore implements ReservationStore, PanacheRepositoryBase<ReservationEntity, UUID> {

    /** Crea el adaptador; lo instancia CDI. */
    public PanacheReservationStore() {}

    @Override
    public Optional<Reservation> lock(UUID id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE).map(ReservationEntity::toDomain);
    }

    @Override
    public void save(Reservation reservation) {
        ReservationEntity existing = findById(reservation.id());
        if (existing == null) {
            persist(ReservationEntity.of(reservation));
        } else {
            existing.status = reservation.status();
        }
    }
}
