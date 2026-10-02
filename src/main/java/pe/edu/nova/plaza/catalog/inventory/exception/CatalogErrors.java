package pe.edu.nova.plaza.catalog.inventory.exception;

import java.util.UUID;
import pe.edu.nova.java.libs.api.standard.error.DomainError;

/**
 * Los errores del dominio del catálogo (ADR-031). Cada uno lleva su código, y la extensión del estándar de API
 * de Nova lo responde con el sobre y el status de su tipo.
 */
public final class CatalogErrors {

    /** El producto no existe. */
    public static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";

    /** No alcanza el stock para la reserva. */
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";

    /** Una reserva no puede mezclar monedas. */
    public static final String MIXED_CURRENCIES = "MIXED_CURRENCIES";

    /** La reserva no existe. */
    public static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";

    /** La reserva venció antes de confirmarse. */
    public static final String RESERVATION_EXPIRED = "RESERVATION_EXPIRED";

    /** La reserva ya se liberó, así que no se puede confirmar. */
    public static final String RESERVATION_RELEASED = "RESERVATION_RELEASED";

    /** La reserva ya se confirmó, así que no se puede liberar. */
    public static final String RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED";

    private CatalogErrors() {}

    /**
     * El producto no existe: 404.
     *
     * @param sku el código pedido
     * @return el error
     */
    public static DomainError productNotFound(String sku) {
        return DomainError.notFound(PRODUCT_NOT_FOUND, "El producto " + sku + " no existe");
    }

    /**
     * No alcanza el stock: 409.
     *
     * @param sku       el producto
     * @param requested las unidades pedidas
     * @param available las unidades que se pueden reservar
     * @return el error
     */
    public static DomainError outOfStock(String sku, int requested, int available) {
        return DomainError.conflict(
                OUT_OF_STOCK,
                "No hay stock suficiente de " + sku + ": se pidieron " + requested + " y hay " + available);
    }

    /**
     * La reserva mezcla monedas: 422.
     *
     * @return el error
     */
    public static DomainError mixedCurrencies() {
        return DomainError.ruleViolation(MIXED_CURRENCIES, "Una reserva no puede mezclar productos de distintas monedas");
    }

    /**
     * La reserva no existe: 404.
     *
     * @param id la reserva pedida
     * @return el error
     */
    public static DomainError reservationNotFound(UUID id) {
        return DomainError.notFound(RESERVATION_NOT_FOUND, "La reserva " + id + " no existe");
    }

    /**
     * La reserva venció: 409.
     *
     * @param id la reserva
     * @return el error
     */
    public static DomainError reservationExpired(UUID id) {
        return DomainError.conflict(RESERVATION_EXPIRED, "La reserva " + id + " venció y ya no aparta stock");
    }

    /**
     * La reserva ya se liberó: 409.
     *
     * @param id la reserva
     * @return el error
     */
    public static DomainError reservationReleased(UUID id) {
        return DomainError.conflict(RESERVATION_RELEASED, "La reserva " + id + " ya se liberó");
    }

    /**
     * La reserva ya se confirmó: 409.
     *
     * @param id la reserva
     * @return el error
     */
    public static DomainError reservationConfirmed(UUID id) {
        return DomainError.conflict(RESERVATION_CONFIRMED, "La reserva " + id + " ya se confirmó");
    }
}
