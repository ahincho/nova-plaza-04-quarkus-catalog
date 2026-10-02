package pe.edu.nova.plaza.catalog.inventory.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Un producto del catálogo, con su precio y las unidades que hay en el almacén.
 *
 * <p>El stock es lo que existe físicamente. Lo que se puede vender es el stock menos lo que apartan las
 * reservas vigentes, y eso lo calcula {@link #available(int)} con lo apartado que le pasa el caso de uso.
 *
 * @param sku      el código del producto
 * @param name     el nombre que ve el cliente
 * @param price    el precio de una unidad
 * @param currency la moneda, en ISO 4217
 * @param stock    las unidades en el almacén
 */
public record Product(String sku, String name, BigDecimal price, String currency, int stock) {

    /**
     * Crea el producto.
     *
     * @throws IllegalArgumentException si el stock es negativo
     */
    public Product {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(currency, "currency");
        if (stock < 0) {
            throw new IllegalArgumentException("el stock no puede ser negativo: " + stock);
        }
    }

    /**
     * Las unidades que se pueden reservar.
     *
     * @param held las unidades que apartan las reservas vigentes
     * @return el stock menos lo apartado, nunca negativo
     */
    public int available(int held) {
        return Math.max(0, stock - held);
    }

    /**
     * El producto después de entregar unidades de una reserva confirmada.
     *
     * @param quantity las unidades que salen del almacén
     * @return el producto con el stock descontado
     * @throws IllegalArgumentException si salen más unidades de las que hay
     */
    public Product withdraw(int quantity) {
        return new Product(sku, name, price, currency, stock - quantity);
    }
}
