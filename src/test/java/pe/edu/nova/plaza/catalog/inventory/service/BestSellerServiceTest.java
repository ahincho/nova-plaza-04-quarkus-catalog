package pe.edu.nova.plaza.catalog.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.plaza.catalog.inventory.domain.BestSeller;
import pe.edu.nova.plaza.catalog.inventory.domain.Sale;
import pe.edu.nova.plaza.catalog.inventory.port.out.ProcessedEvents;
import pe.edu.nova.plaza.catalog.inventory.port.out.SalesStore;

class BestSellerServiceTest {

    private final Set<String> processed = new HashSet<>();
    private final Map<String, Long> units = new TreeMap<>();
    private final List<List<Sale>> additions = new ArrayList<>();

    private final ProcessedEvents events = (source, id) -> processed.add(source + "|" + id);
    private final SalesStore store = new SalesStore() {
        @Override
        public void add(List<Sale> sales) {
            additions.add(sales);
            sales.forEach(sale -> units.merge(sale.sku(), (long) sale.quantity(), Long::sum));
        }

        @Override
        public List<BestSeller> top(int limit) {
            return units.entrySet().stream()
                    .map(entry -> new BestSeller(entry.getKey(), entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparingLong(BestSeller::unitsSold).reversed())
                    .limit(limit)
                    .toList();
        }
    };
    private final BestSellerService service = new BestSellerService(events, store);

    @Test
    void aConfirmedOrderAddsItsUnitsOnceEvenIfTheEventRepeats() {
        assertTrue(service.record("/plaza/orders", "event-1", List.of(new Sale("MUG-001", 2))));
        assertFalse(service.record("/plaza/orders", "event-1", List.of(new Sale("MUG-001", 2))));
        assertTrue(service.record("/plaza/orders", "event-2", List.of(new Sale("MUG-001", 1), new Sale("TEE-002", 5))));

        assertEquals(List.of(new BestSeller("TEE-002", "TEE-002", 5), new BestSeller("MUG-001", "MUG-001", 3)),
                service.top(10));
    }

    @Test
    void aRepeatedProductIsOneSumInTheOrderOfItsCode() {
        service.record("/plaza/orders", "event-1",
                List.of(new Sale("TEE-002", 1), new Sale("MUG-001", 2), new Sale("TEE-002", 3)));

        assertEquals(List.of(List.of(new Sale("MUG-001", 2), new Sale("TEE-002", 4))), additions);
    }

    @Test
    void theLimitGoesFromOneToFifty() {
        ApplicationError error = assertThrows(ApplicationError.class, () -> service.top(0));

        assertEquals("limit", error.fieldErrors().get(0).field());
        assertThrows(ApplicationError.class, () -> service.top(51));
        assertEquals(List.of(), service.top(50));
    }

    @Test
    void aSaleNeedsAProductAndPositiveUnits() {
        assertThrows(IllegalArgumentException.class, () -> new Sale(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new Sale("MUG-001", 0));
    }
}
