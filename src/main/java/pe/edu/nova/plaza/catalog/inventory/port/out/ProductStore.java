package pe.edu.nova.plaza.catalog.inventory.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;
import pe.edu.nova.plaza.catalog.inventory.domain.Product;

/** Los productos guardados, en el lenguaje del catálogo. */
public interface ProductStore {

    /**
     * Una página de productos, ordenada por código.
     *
     * @param request cuántos productos y desde qué cursor
     * @return la página
     */
    CursorPage<Product> page(CursorRequest request);

    /**
     * Un producto.
     *
     * @param sku el código
     * @return el producto, si existe
     */
    Optional<Product> find(String sku);

    /**
     * Toma los productos para cambiarlos: dos reservas del mismo producto no pueden apartar a la vez el último
     * stock.
     *
     * @param skus los códigos
     * @return los que existen, ordenados por código para que dos tomas nunca se crucen
     */
    List<Product> lock(Collection<String> skus);

    /**
     * Las unidades que apartan las reservas vigentes de cada producto.
     *
     * @param skus los códigos
     * @param now  el momento de la consulta: una reserva vencida ya no aparta nada
     * @return lo apartado por código; un producto sin reservas no aparece
     */
    Map<String, Integer> held(Collection<String> skus, Instant now);

    /**
     * Guarda el stock de un producto.
     *
     * @param product el producto con su stock nuevo
     */
    void save(Product product);
}
