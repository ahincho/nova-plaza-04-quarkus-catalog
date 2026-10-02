package pe.edu.nova.plaza.catalog.inventory.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una línea de una reserva, con el precio del momento en que se reservó.
 *
 * @param sku       el código del producto
 * @param quantity  las unidades apartadas
 * @param unitPrice el precio de una unidad al reservar
 */
public record ReservationLine(String sku, int quantity, BigDecimal unitPrice) {

    /**
     * Crea la línea.
     *
     * @throws IllegalArgumentException si la cantidad no es positiva
     */
    public ReservationLine {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity < 1) {
            throw new IllegalArgumentException("la cantidad tiene que ser positiva: " + quantity);
        }
    }

    /**
     * El precio de la línea.
     *
     * @return el precio unitario por la cantidad
     */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
