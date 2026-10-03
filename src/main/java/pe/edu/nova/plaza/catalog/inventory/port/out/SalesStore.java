package pe.edu.nova.plaza.catalog.inventory.port.out;

import java.util.List;
import pe.edu.nova.plaza.catalog.inventory.domain.BestSeller;
import pe.edu.nova.plaza.catalog.inventory.domain.Sale;

/** Las unidades vendidas de cada producto. */
public interface SalesStore {

    /**
     * Suma las ventas, en la transacción vigente.
     *
     * @param sales las ventas
     */
    void add(List<Sale> sales);

    /**
     * Los productos más vendidos.
     *
     * @param limit cuántos
     * @return de más a menos unidades, con el código como desempate
     */
    List<BestSeller> top(int limit);
}
