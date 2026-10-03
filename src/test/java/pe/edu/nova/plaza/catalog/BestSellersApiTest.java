package pe.edu.nova.plaza.catalog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import pe.edu.nova.plaza.catalog.inventory.adapter.in.messaging.OrderEventsConsumer;

/**
 * El ranking contra un Postgres de verdad. Las pruebas no levantan Kafka: le entregan al consumidor el registro tal
 * como lo publica Debezium, con las cabeceras de CloudEvents.
 */
@QuarkusTest
class BestSellersApiTest {

    private static final String CONFIRMED = "pe.edu.nova.plaza.order.confirmed.v1";

    @Inject
    OrderEventsConsumer consumer;

    @Test
    void eachConfirmedOrderAddsItsUnitsOnceAndTheOtherEventsAreIgnored() throws Exception {
        String repeated = UUID.randomUUID().toString();
        consumer.consume(record(repeated, CONFIRMED, "{\"items\":[{\"sku\":\"BAG-004\",\"quantity\":4}]}"));
        consumer.consume(record(repeated, CONFIRMED, "{\"items\":[{\"sku\":\"BAG-004\",\"quantity\":4}]}"));
        consumer.consume(record(UUID.randomUUID().toString(), CONFIRMED,
                "{\"items\":[{\"sku\":\"BAG-004\",\"quantity\":1},{\"sku\":\"CAP-003\",\"quantity\":2}]}"));
        consumer.consume(record(UUID.randomUUID().toString(), "pe.edu.nova.plaza.order.created.v1",
                "{\"items\":[{\"sku\":\"CAP-003\",\"quantity\":50}]}"));

        given().queryParam("limit", 2)
                .get("/v1/products/best-sellers")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data[0].sku", equalTo("BAG-004"))
                .body("data[0].unitsSold", equalTo(5))
                .body("data[0].name", equalTo(nameOf("BAG-004")))
                .body("data[1].sku", equalTo("CAP-003"))
                .body("data[1].unitsSold", equalTo(2));
    }

    @Test
    void anEventWithoutItsIdStopsTheConsumerInsteadOfBeingSkipped() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("plaza.orders", 0, 0, "order-1", "{}");
        record.headers().add("ce_type", CONFIRMED.getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> consumer.consume(record));
    }

    @Test
    void theLimitOfTheRankingIsValidated() {
        given().queryParam("limit", 0)
                .get("/v1/products/best-sellers")
                .then()
                .statusCode(400)
                .body("errors[0].field", equalTo("limit"));
    }

    private static String nameOf(String sku) {
        return given().get("/v1/products/{sku}", sku).then().statusCode(200).extract().path("data.name");
    }

    private static ConsumerRecord<String, String> record(String id, String type, String payload) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("plaza.orders", 0, 0, "order-1", payload);
        record.headers().add("ce_id", id.getBytes(StandardCharsets.UTF_8));
        record.headers().add("ce_source", "/plaza/orders".getBytes(StandardCharsets.UTF_8));
        record.headers().add("ce_type", type.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
