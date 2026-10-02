package pe.edu.nova.plaza.catalog.inventory.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationStatus;

/** La fila de una reserva, con sus líneas. */
@Entity
@Table(name = "reservation")
public class ReservationEntity {

    @Id
    UUID id;

    @Column(name = "customer_id", nullable = false, length = 64)
    String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    ReservationStatus status;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Version
    long version;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sku")
    List<ReservationLineEntity> lines = new ArrayList<>();

    /** Para JPA. */
    protected ReservationEntity() {}

    static ReservationEntity of(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity();
        entity.id = reservation.id();
        entity.customerId = reservation.customerId();
        entity.status = reservation.status();
        entity.currency = reservation.currency();
        entity.createdAt = reservation.createdAt();
        entity.expiresAt = reservation.expiresAt();
        reservation.lines().forEach(line -> entity.lines.add(ReservationLineEntity.of(entity, line)));
        return entity;
    }

    Reservation toDomain() {
        return new Reservation(
                id,
                customerId,
                status,
                currency,
                createdAt,
                expiresAt,
                lines.stream().map(ReservationLineEntity::toDomain).toList());
    }
}
