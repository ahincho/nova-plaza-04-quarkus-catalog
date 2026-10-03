package pe.edu.nova.plaza.catalog.inventory.adapter.in.web.response;

import pe.edu.nova.plaza.catalog.inventory.domain.BestSeller;

/**
 * Un producto del ranking, como lo ve el cliente.
 *
 * @param sku       el código
 * @param name      el nombre
 * @param unitsSold las unidades vendidas
 */
public record BestSellerResponse(String sku, String name, long unitsSold) {

    /**
     * Arma la respuesta.
     *
     * @param bestSeller el producto del ranking
     * @return la respuesta
     */
    public static BestSellerResponse of(BestSeller bestSeller) {
        return new BestSellerResponse(bestSeller.sku(), bestSeller.name(), bestSeller.unitsSold());
    }
}
