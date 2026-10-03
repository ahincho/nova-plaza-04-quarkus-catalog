package pe.edu.nova.plaza.catalog.inventory.adapter.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import pe.edu.nova.plaza.catalog.inventory.domain.BestSeller;
import pe.edu.nova.plaza.catalog.inventory.domain.Sale;
import pe.edu.nova.plaza.catalog.inventory.port.out.SalesStore;

/**
 * Las unidades vendidas sobre Postgres. Sumar es un {@code insert ... on conflict do update}: una sola sentencia
 * por producto, sin leer antes, así que dos eventos a la vez no pierden ninguna suma.
 */
@ApplicationScoped
public class NativeSalesStore implements SalesStore {

    private final EntityManager entityManager;

    /**
     * Crea el adaptador.
     *
     * @param entityManager el de la transacción vigente
     */
    public NativeSalesStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(List<Sale> sales) {
        for (Sale sale : sales) {
            entityManager
                    .createNativeQuery("insert into best_seller (sku, units_sold) values (?1, ?2) "
                            + "on conflict (sku) do update set units_sold = best_seller.units_sold + excluded.units_sold")
                    .setParameter(1, sale.sku())
                    .setParameter(2, sale.quantity())
                    .executeUpdate();
        }
    }

    @Override
    public List<BestSeller> top(int limit) {
        List<?> rows = entityManager
                .createNativeQuery(
                        "select b.sku, p.name, b.units_sold from best_seller b join product p on p.sku = b.sku "
                                + "order by b.units_sold desc, b.sku limit ?1",
                        Object[].class)
                .setParameter(1, limit)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(row -> new BestSeller((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();
    }
}
