package pe.edu.nova.plaza.catalog.inventory.adapter.in.web.response;

import java.math.BigDecimal;
import pe.edu.nova.plaza.catalog.inventory.port.in.ProductView;

/**
 * Un producto en JSON.
 *
 * @param sku       el código
 * @param name      el nombre
 * @param price     el precio de una unidad
 * @param currency  la moneda
 * @param available las unidades que se pueden reservar ahora
 */
public record ProductResponse(String sku, String name, BigDecimal price, String currency, int available) {

    /**
     * Convierte la vista del caso de uso.
     *
     * @param view la vista
     * @return la respuesta
     */
    public static ProductResponse of(ProductView view) {
        return new ProductResponse(view.sku(), view.name(), view.price(), view.currency(), view.available());
    }
}
