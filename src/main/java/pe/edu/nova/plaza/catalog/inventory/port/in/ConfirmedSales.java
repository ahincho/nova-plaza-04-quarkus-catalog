package pe.edu.nova.plaza.catalog.inventory.port.in;

import java.util.List;
import pe.edu.nova.plaza.catalog.inventory.domain.Sale;

/** Suma al ranking las ventas de un pedido confirmado, que llegan como evento de pedidos (ADR-048). */
public interface ConfirmedSales {

    /**
     * Registra las ventas de un evento. El evento llega al menos una vez: uno repetido no suma de nuevo.
     *
     * @param source  el {@code ce_source} del evento
     * @param eventId el {@code ce_id} del evento
     * @param sales   las ventas del pedido
     * @return {@code true} si las sumó, y {@code false} si el evento ya se había procesado
     */
    boolean record(String source, String eventId, List<Sale> sales);
}
