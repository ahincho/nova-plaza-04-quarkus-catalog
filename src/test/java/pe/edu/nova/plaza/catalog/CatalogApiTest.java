package pe.edu.nova.plaza.catalog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * La API del catálogo contra un Postgres de verdad, que Quarkus levanta con Dev Services, con los productos de la
 * migración. Cada respuesta llega en el sobre de Nova.
 */
@QuarkusTest
class CatalogApiTest {

    private static final String CUSTOMER = "X-Customer-Id";

    @Test
    void theCatalogScrollsByCodeWithTheSameCursorContractAsTheOrders() {
        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            var request = given().queryParam("limit", 5);
            if (cursor != null) {
                request.queryParam("cursor", cursor);
            }
            var page = request.get("/v1/products")
                    .then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .extract()
                    .jsonPath();
            seen.addAll(page.getList("data.items.sku", String.class));
            cursor = page.getString("data.nextCursor");
            pages++;
        } while (cursor != null);

        org.junit.jupiter.api.Assertions.assertEquals(3, pages);
        org.junit.jupiter.api.Assertions.assertEquals(12, seen.size());
        org.junit.jupiter.api.Assertions.assertEquals(seen.stream().sorted().toList(), seen);
    }

    @Test
    void aBadCursorOrLimitIs400OnItsField() {
        given().queryParam("cursor", "not-a-cursor").get("/v1/products")
                .then()
                .statusCode(400)
                .body("errors[0].field", equalTo("cursor"));
        given().queryParam("limit", 101).get("/v1/products")
                .then()
                .statusCode(400)
                .body("errors[0].field", equalTo("limit"));
    }

    @Test
    void anUnknownProductIsTheDomainError404() {
        given().get("/v1/products/NOPE-000")
                .then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("PRODUCT_NOT_FOUND"));
    }

    @Test
    void aReservationTakesThePricesOfTheCatalogAndIsConfirmedOnce() {
        String id = given().header(CUSTOMER, "customer-1")
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"sku\":\"MUG-001\",\"quantity\":2},{\"sku\":\"NOTE-005\",\"quantity\":1}]}")
                .post("/v1/reservations")
                .then()
                .statusCode(201)
                .body("data.status", equalTo("HELD"))
                .body("data.currency", equalTo("PEN"))
                .body("data.total", equalTo(66.00f))
                .body("data.expiresAt", notNullValue())
                .body("data.items.sku", hasItems("MUG-001", "NOTE-005"))
                .extract()
                .path("data.id");

        given().post("/v1/reservations/{id}/confirm", id)
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CONFIRMED"));
        given().post("/v1/reservations/{id}/confirm", id)
                .then()
                .statusCode(200)
                .body("data.status", equalTo("CONFIRMED"));
        given().post("/v1/reservations/{id}/release", id)
                .then()
                .statusCode(409)
                .body("errors[0].code", equalTo("RESERVATION_CONFIRMED"));
    }

    @Test
    void theLastUnitsGoToTheFirstReservationAndTheSecondIs409() {
        String id = given().header(CUSTOMER, "customer-2")
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"sku\":\"HOOD-008\",\"quantity\":3}]}")
                .post("/v1/reservations")
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");

        given().header(CUSTOMER, "customer-3")
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"sku\":\"HOOD-008\",\"quantity\":1}]}")
                .post("/v1/reservations")
                .then()
                .statusCode(409)
                .body("errors[0].code", equalTo("OUT_OF_STOCK"));

        given().post("/v1/reservations/{id}/release", id)
                .then()
                .statusCode(200)
                .body("data.status", equalTo("RELEASED"));
        given().get("/v1/products/HOOD-008")
                .then()
                .statusCode(200)
                .body("data.available", equalTo(3));
    }

    @Test
    void anInvalidReservationIs400WithItsFields() {
        given().header(CUSTOMER, "customer-4")
                .contentType(ContentType.JSON)
                .body("{\"items\":[]}")
                .post("/v1/reservations")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors[0].code", equalTo("BAD_REQUEST"));
    }

    @Test
    void theSamePurchaseKeyGetsTheSameReservation() {
        String body = "{\"items\":[{\"sku\":\"PEN-006\",\"quantity\":2}]}";
        String first = given().header(CUSTOMER, "customer-5")
                .header("Idempotency-Key", "purchase-5")
                .contentType(ContentType.JSON)
                .body(body)
                .post("/v1/reservations")
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");

        given().header(CUSTOMER, "customer-5")
                .header("Idempotency-Key", "purchase-5")
                .contentType(ContentType.JSON)
                .body(body)
                .post("/v1/reservations")
                .then()
                .statusCode(201)
                .body("data.id", equalTo(first));
        given().header(CUSTOMER, "customer-5")
                .header("Idempotency-Key", "purchase-5")
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"sku\":\"PEN-006\",\"quantity\":3}]}")
                .post("/v1/reservations")
                .then()
                .statusCode(422)
                .body("errors[0].code", equalTo("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void theLastPageSaysItHasNoNextCursor() {
        given().queryParam("limit", 100).get("/v1/products")
                .then()
                .statusCode(200)
                .body("data.hasNext", equalTo(false))
                .body("data.nextCursor", nullValue());
    }
}
