package pe.edu.nova.plaza.catalog.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationLine;

/** La fila de una línea de reserva. */
@Entity
@Table(name = "reservation_line")
public class ReservationLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reservation_id")
    ReservationEntity reservation;

    @Column(nullable = false, length = 64)
    String sku;

    @Column(nullable = false)
    int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    BigDecimal unitPrice;

    /** Para JPA. */
    protected ReservationLineEntity() {}

    static ReservationLineEntity of(ReservationEntity reservation, ReservationLine line) {
        ReservationLineEntity entity = new ReservationLineEntity();
        entity.reservation = reservation;
        entity.sku = line.sku();
        entity.quantity = line.quantity();
        entity.unitPrice = line.unitPrice();
        return entity;
    }

    ReservationLine toDomain() {
        return new ReservationLine(sku, quantity, unitPrice);
    }
}
