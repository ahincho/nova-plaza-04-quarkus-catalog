package pe.edu.nova.plaza.catalog.inventory.port.out;

/** Los eventos que el catálogo ya procesó: lo que lo deduplica, porque un evento llega al menos una vez. */
public interface ProcessedEvents {

    /**
     * Registra el evento en la transacción vigente.
     *
     * @param source el {@code ce_source} del evento
     * @param id     el {@code ce_id} del evento
     * @return {@code true} si es la primera vez
     */
    boolean firstTime(String source, String id);
}
