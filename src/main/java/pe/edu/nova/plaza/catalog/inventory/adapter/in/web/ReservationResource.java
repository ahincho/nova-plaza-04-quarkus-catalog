package pe.edu.nova.plaza.catalog.inventory.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import pe.edu.nova.plaza.catalog.inventory.adapter.in.web.request.CreateReservationRequest;
import pe.edu.nova.plaza.catalog.inventory.adapter.in.web.response.ReservationResponse;
import pe.edu.nova.plaza.catalog.inventory.port.in.StockReservations;

/**
 * Las reservas de stock. Las llama solo el BFF, que ya validó el token y pasa el cliente en
 * {@value #CUSTOMER_HEADER}, como en pedidos.
 */
@Path("/v1/reservations")
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

    /** El cliente que el BFF autenticó. */
    public static final String CUSTOMER_HEADER = "X-Customer-Id";

    /** La clave de la compra: repetirla con los mismos productos devuelve la misma reserva. */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final StockReservations reservations;

    /**
     * Crea el recurso.
     *
     * @param reservations los casos de uso de las reservas
     */
    public ReservationResource(StockReservations reservations) {
        this.reservations = reservations;
    }

    /**
     * Aparta el stock de una compra y devuelve los precios del momento. Sin stock suficiente, 409
     * {@code OUT_OF_STOCK}.
     *
     * @param customerId     el cliente
     * @param idempotencyKey la clave de la compra, opcional
     * @param request        los productos y sus cantidades
     * @return la reserva, con 201
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reserve(
            @HeaderParam(CUSTOMER_HEADER) @NotBlank String customerId,
            @HeaderParam(IDEMPOTENCY_HEADER) String idempotencyKey,
            @NotNull @Valid CreateReservationRequest request) {
        ReservationResponse body = ReservationResponse.of(
                reservations.reserve(customerId, blankToNull(idempotencyKey), request.toItems()));
        return Response.status(Response.Status.CREATED).entity(body).build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * Confirma la reserva: el stock sale del almacén.
     *
     * @param id la reserva
     * @return la reserva confirmada
     */
    @POST
    @Path("/{id}/confirm")
    public ReservationResponse confirm(@PathParam("id") UUID id) {
        return ReservationResponse.of(reservations.confirm(id));
    }

    /**
     * Libera la reserva: el stock vuelve a estar disponible.
     *
     * @param id la reserva
     * @return la reserva liberada
     */
    @POST
    @Path("/{id}/release")
    public ReservationResponse release(@PathParam("id") UUID id) {
        return ReservationResponse.of(reservations.release(id));
    }
}
