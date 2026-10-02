package pe.edu.nova.plaza.catalog.inventory.domain;

/** El estado de una reserva. */
public enum ReservationStatus {

    /** Aparta stock hasta que vence, se confirma o se libera. */
    HELD,

    /** La compra terminó y el stock salió del almacén. */
    CONFIRMED,

    /** La compra no siguió y el stock volvió a estar disponible. */
    RELEASED
}
