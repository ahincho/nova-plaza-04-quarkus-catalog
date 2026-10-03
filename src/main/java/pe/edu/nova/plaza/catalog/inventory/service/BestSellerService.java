package pe.edu.nova.plaza.catalog.inventory.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.FieldError;
import pe.edu.nova.plaza.catalog.inventory.domain.BestSeller;
import pe.edu.nova.plaza.catalog.inventory.domain.Sale;
import pe.edu.nova.plaza.catalog.inventory.port.in.BestSellers;
import pe.edu.nova.plaza.catalog.inventory.port.in.ConfirmedSales;
import pe.edu.nova.plaza.catalog.inventory.port.out.ProcessedEvents;
import pe.edu.nova.plaza.catalog.inventory.port.out.SalesStore;

/**
 * El ranking de lo más vendido, que se alimenta de los pedidos confirmados (ADR-043 y ADR-048).
 *
 * <p>El registro del evento y la suma de sus ventas van en la misma transacción: si la suma falla, el evento
 * queda sin registrar y se vuelve a procesar; si el evento se repite, no suma de nuevo.
 */
@ApplicationScoped
public class BestSellerService implements ConfirmedSales, BestSellers {

    private final ProcessedEvents processed;
    private final SalesStore sales;

    /**
     * Crea el caso de uso.
     *
     * @param processed los eventos ya procesados
     * @param sales     las unidades vendidas
     */
    public BestSellerService(ProcessedEvents processed, SalesStore sales) {
        this.processed = processed;
        this.sales = sales;
    }

    @Override
    @Transactional
    public boolean record(String source, String eventId, List<Sale> sold) {
        if (!processed.firstTime(source, eventId)) {
            return false;
        }
        // Un producto repetido en el pedido es una sola suma, y el orden por código evita que dos sumas se crucen.
        Map<String, Integer> bySku =
                sold.stream().collect(Collectors.toMap(Sale::sku, Sale::quantity, Integer::sum, TreeMap::new));
        sales.add(bySku.entrySet().stream()
                .map(entry -> new Sale(entry.getKey(), entry.getValue()))
                .toList());
        return true;
    }

    @Override
    public List<BestSeller> top(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw ApplicationError.invalidInput(
                    "El límite del ranking va de 1 a " + MAX_LIMIT,
                    List.of(FieldError.of("limit", "Debe ir de 1 a " + MAX_LIMIT)));
        }
        return sales.top(limit);
    }
}
