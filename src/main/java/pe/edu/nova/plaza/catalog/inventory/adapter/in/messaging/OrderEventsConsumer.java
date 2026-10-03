package pe.edu.nova.plaza.catalog.inventory.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import pe.edu.nova.plaza.catalog.inventory.domain.Sale;
import pe.edu.nova.plaza.catalog.inventory.port.in.ConfirmedSales;

/**
 * Lee los eventos de pedidos de {@code plaza.orders}, que Debezium publica como CloudEvents en modo binario
 * (ADR-048), y suma al ranking las ventas de cada pedido confirmado. Los demás tipos no le tocan.
 *
 * <p>Un evento que no se puede procesar detiene el consumo, que es la estrategia por defecto de SmallRye: nunca se
 * salta uno en silencio. La traza continúa sola desde la cabecera {@code traceparent}.
 */
@ApplicationScoped
public class OrderEventsConsumer {

    /** El tipo del evento que alimenta el ranking. */
    static final String CONFIRMED = "pe.edu.nova.plaza.order.confirmed.v1";

    private static final Logger LOG = Logger.getLogger(OrderEventsConsumer.class);

    private final ConfirmedSales sales;
    private final ObjectMapper json;

    /**
     * Crea el consumidor.
     *
     * @param sales el caso de uso del ranking
     * @param json  el mapper del servicio
     */
    public OrderEventsConsumer(ConfirmedSales sales, ObjectMapper json) {
        this.sales = sales;
        this.json = json;
    }

    /**
     * Procesa un registro de {@code plaza.orders}.
     *
     * @param record el registro, con las cabeceras de CloudEvents
     * @throws IOException si el payload no es JSON
     */
    @Incoming("orders")
    @Blocking
    public void consume(ConsumerRecord<String, String> record) throws IOException {
        String type = header(record, "ce_type");
        if (!CONFIRMED.equals(type)) {
            return;
        }
        String source = required(record, "ce_source");
        String id = required(record, "ce_id");
        JsonNode payload = json.readTree(record.value());
        List<Sale> sold = new ArrayList<>();
        for (JsonNode item : payload.path("items")) {
            sold.add(new Sale(item.path("sku").asText(), item.path("quantity").asInt()));
        }
        boolean counted = sales.record(source, id, sold);
        // Sin el payload: solo el id del evento y si sumó.
        LOG.debugf("Evento %s %s", id, counted ? "sumado al ranking" : "repetido, descartado");
    }

    private static String required(ConsumerRecord<String, String> record, String name) {
        String value = header(record, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The order event has no " + name + " header");
        }
        return value;
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null || header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
