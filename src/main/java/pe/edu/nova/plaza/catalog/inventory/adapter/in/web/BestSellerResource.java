package pe.edu.nova.plaza.catalog.inventory.adapter.in.web;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import pe.edu.nova.plaza.catalog.inventory.adapter.in.web.response.BestSellerResponse;
import pe.edu.nova.plaza.catalog.inventory.port.in.BestSellers;

/** El ranking de lo más vendido, que se alimenta de los pedidos confirmados. */
@Path("/v1/products/best-sellers")
@Produces(MediaType.APPLICATION_JSON)
public class BestSellerResource {

    private final BestSellers bestSellers;

    /**
     * Crea el recurso.
     *
     * @param bestSellers el ranking
     */
    public BestSellerResource(BestSellers bestSellers) {
        this.bestSellers = bestSellers;
    }

    /**
     * Los productos más vendidos.
     *
     * @param limit cuántos, diez por defecto y cincuenta como máximo
     * @return de más a menos unidades vendidas
     */
    @GET
    public List<BestSellerResponse> top(@QueryParam("limit") @DefaultValue("10") int limit) {
        return bestSellers.top(limit).stream().map(BestSellerResponse::of).toList();
    }
}
