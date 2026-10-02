package pe.edu.nova.plaza.catalog.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/** El reloj del servicio. Una prueba lo reemplaza para decidir cuándo vence una reserva. */
@ApplicationScoped
public class ClockProducer {

    /** Crea el productor; lo instancia CDI. */
    public ClockProducer() {}

    /**
     * El reloj del sistema, en UTC.
     *
     * @return el reloj
     */
    @Produces
    @ApplicationScoped
    public Clock clock() {
        return Clock.systemUTC();
    }
}
