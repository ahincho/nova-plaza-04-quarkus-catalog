package pe.edu.nova.plaza.catalog.inventory.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import pe.edu.nova.plaza.catalog.inventory.port.in.StockReservations;

/**
 * El cuerpo de una reserva: los productos y sus cantidades. El precio no viaja: lo pone el catálogo.
 *
 * @param items las líneas
 */
public record CreateReservationRequest(@NotEmpty List<@NotNull @Valid Item> items) {

    /**
     * Las líneas, en el lenguaje del caso de uso.
     *
     * @return los productos y sus cantidades
     */
    public List<StockReservations.Item> toItems() {
        return items.stream().map(item -> new StockReservations.Item(item.sku(), item.quantity())).toList();
    }

    /**
     * Una línea.
     *
     * @param sku      el código del producto
     * @param quantity las unidades
     */
    public record Item(@NotBlank String sku, @Positive int quantity) {}
}
