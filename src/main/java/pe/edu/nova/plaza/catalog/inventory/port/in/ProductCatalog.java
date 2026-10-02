package pe.edu.nova.plaza.catalog.inventory.port.in;

import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;

/** Los casos de uso de lectura del catálogo. */
public interface ProductCatalog {

    /**
     * Una página de productos, por código, para un scroll infinito (ADR-054).
     *
     * @param request cuántos productos y desde qué cursor
     * @return la página
     */
    CursorPage<ProductView> page(CursorRequest request);

    /**
     * Un producto.
     *
     * @param sku el código
     * @return el producto, con lo que se puede reservar ahora
     */
    ProductView find(String sku);
}
