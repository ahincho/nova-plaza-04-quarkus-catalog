package pe.edu.nova.plaza.catalog.inventory.port.in;

import java.util.List;
import pe.edu.nova.plaza.catalog.inventory.domain.BestSeller;

/** El ranking de lo más vendido. */
public interface BestSellers {

    /** Cuántos productos devuelve el ranking si no se pide otra cosa. */
    int DEFAULT_LIMIT = 10;

    /** Cuántos productos devuelve como máximo. */
    int MAX_LIMIT = 50;

    /**
     * Los productos más vendidos, de más a menos unidades, con el código como desempate.
     *
     * @param limit cuántos, entre 1 y {@link #MAX_LIMIT}
     * @return el ranking
     */
    List<BestSeller> top(int limit);
}
