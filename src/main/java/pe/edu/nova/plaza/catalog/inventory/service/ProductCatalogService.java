package pe.edu.nova.plaza.catalog.inventory.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;
import pe.edu.nova.plaza.catalog.inventory.domain.Product;
import pe.edu.nova.plaza.catalog.inventory.exception.CatalogErrors;
import pe.edu.nova.plaza.catalog.inventory.port.in.ProductCatalog;
import pe.edu.nova.plaza.catalog.inventory.port.in.ProductView;
import pe.edu.nova.plaza.catalog.inventory.port.out.ProductStore;

/** Responde los productos con lo que se puede reservar de cada uno en este momento. */
@ApplicationScoped
public class ProductCatalogService implements ProductCatalog {

    private final ProductStore products;
    private final Clock clock;

    /**
     * Crea el caso de uso.
     *
     * @param products los productos guardados
     * @param clock    el reloj con el que se decide qué reservas siguen vigentes
     */
    public ProductCatalogService(ProductStore products, Clock clock) {
        this.products = products;
        this.clock = clock;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public CursorPage<ProductView> page(CursorRequest request) {
        CursorPage<Product> page = products.page(request);
        List<String> skus = page.items().stream().map(Product::sku).toList();
        Map<String, Integer> held = products.held(skus, clock.instant());
        return page.map(product -> view(product, held));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public ProductView find(String sku) {
        Product product = products.find(sku).orElseThrow(() -> CatalogErrors.productNotFound(sku));
        return view(product, products.held(List.of(sku), clock.instant()));
    }

    private static ProductView view(Product product, Map<String, Integer> held) {
        return new ProductView(
                product.sku(),
                product.name(),
                product.price(),
                product.currency(),
                product.available(held.getOrDefault(product.sku(), 0)));
    }
}
