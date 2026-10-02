package pe.edu.nova.plaza.catalog.inventory.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pe.edu.nova.java.libs.persistence.CursorCodec;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;
import pe.edu.nova.plaza.catalog.inventory.domain.Product;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationStatus;
import pe.edu.nova.plaza.catalog.inventory.port.out.ProductStore;

/**
 * Los productos sobre Postgres, con Panache.
 *
 * <p>La página es por keyset (ADR-054): ordena por código, que es la clave primaria y nunca se repite, y la
 * siguiente página pide los códigos mayores al último entregado. El cursor es el mismo de pedidos, escrito y
 * leído por el núcleo puro de {@code nova-persistence}.
 */
@ApplicationScoped
public class PanacheProductStore implements ProductStore, PanacheRepositoryBase<ProductEntity, String> {

    /** El nombre del orden, que viaja en el cursor y lo ata a esta consulta. */
    static final String SORT = "sku";

    private static final List<String> KEYS = List.of("sku");

    /** Crea el adaptador; lo instancia CDI. */
    public PanacheProductStore() {}

    @Override
    public CursorPage<Product> page(CursorRequest request) {
        String after = request.cursorIfAny()
                .map(cursor -> (String) CursorCodec.decode(cursor, SORT, KEYS).get("sku"))
                .orElse("");
        // Uno más que el límite, para saber si hay otra página sin contar la tabla.
        List<Product> rows = find("sku > ?1", Sort.ascending("sku"), after)
                .range(0, request.limit())
                .list()
                .stream()
                .map(ProductEntity::toDomain)
                .toList();
        if (rows.size() <= request.limit()) {
            return CursorPage.last(rows);
        }
        List<Product> items = rows.subList(0, request.limit());
        String last = items.get(items.size() - 1).sku();
        return CursorPage.of(items, CursorCodec.encode(SORT, Map.of("sku", last)));
    }

    @Override
    public Optional<Product> find(String sku) {
        return findByIdOptional(sku).map(ProductEntity::toDomain);
    }

    @Override
    public List<Product> lock(Collection<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }
        return find("sku in ?1", Sort.ascending("sku"), skus)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .list()
                .stream()
                .map(ProductEntity::toDomain)
                .toList();
    }

    @Override
    public Map<String, Integer> held(Collection<String> skus, Instant now) {
        Map<String, Integer> held = new HashMap<>();
        if (skus.isEmpty()) {
            return held;
        }
        List<Object[]> rows = getEntityManager()
                .createQuery(
                        "select l.sku, sum(l.quantity) from ReservationLineEntity l"
                                + " where l.sku in :skus and l.reservation.status = :held"
                                + " and l.reservation.expiresAt > :now group by l.sku",
                        Object[].class)
                .setParameter("skus", skus)
                .setParameter("held", ReservationStatus.HELD)
                .setParameter("now", now)
                .getResultList();
        rows.forEach(row -> held.put((String) row[0], ((Number) row[1]).intValue()));
        return held;
    }

    @Override
    public void save(Product product) {
        ProductEntity entity = findById(product.sku());
        entity.stock = product.stock();
    }
}
