package pe.edu.nova.plaza.catalog.inventory.adapter.in.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import pe.edu.nova.java.libs.persistence.CursorLimits;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;
import pe.edu.nova.plaza.catalog.inventory.adapter.in.web.response.ProductResponse;
import pe.edu.nova.plaza.catalog.inventory.port.in.ProductCatalog;

/** Los productos del catálogo. La extensión de Nova envuelve cada respuesta en el sobre del estándar de API. */
@Path("/v1/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    private final ProductCatalog catalog;

    /**
     * Crea el recurso.
     *
     * @param catalog los casos de uso de lectura
     */
    public ProductResource(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Una página de productos, por código, con el mismo contrato de scroll que pedidos (ADR-054): {@code ?limit=},
     * 20 por defecto y 100 como máximo, y {@code ?cursor=}.
     *
     * @param limit  cuántos productos
     * @param cursor el {@code nextCursor} de la página anterior
     * @return la página
     */
    @GET
    public CursorPage<ProductResponse> list(@QueryParam("limit") String limit, @QueryParam("cursor") String cursor) {
        return catalog.page(CursorRequest.from(limit, cursor, CursorLimits.DEFAULT)).map(ProductResponse::of);
    }

    /**
     * Un producto, con lo que se puede reservar ahora.
     *
     * @param sku el código
     * @return el producto; si no existe, el error de dominio responde un 404
     */
    @GET
    @Path("/{sku}")
    public ProductResponse find(@PathParam("sku") String sku) {
        return ProductResponse.of(catalog.find(sku));
    }
}
