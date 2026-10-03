package pe.edu.nova.plaza.catalog.inventory.domain;

/**
 * Las unidades de un producto que se vendieron en un pedido confirmado.
 *
 * @param sku      el código del producto
 * @param quantity las unidades, siempre positivas
 */
public record Sale(String sku, int quantity) {

    /** Valida la venta. */
    public Sale {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("A sale needs the sku of its product");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("A sale needs a positive quantity");
        }
    }
}
