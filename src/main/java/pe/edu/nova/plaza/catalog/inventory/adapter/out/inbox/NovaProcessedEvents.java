package pe.edu.nova.plaza.catalog.inventory.adapter.out.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import pe.edu.nova.java.libs.outbox.Inbox;
import pe.edu.nova.java.libs.outbox.jdbc.JdbcInbox;
import pe.edu.nova.plaza.catalog.inventory.port.out.ProcessedEvents;

/**
 * Los eventos procesados, con el inbox de Nova sobre JDBC (ADR-048). La conexión sale del {@code DataSource} de
 * Quarkus: dentro de un método {@code @Transactional}, Agroal la enlista en la transacción JTA, así que el registro
 * se confirma o se revierte con la suma del ranking.
 */
@ApplicationScoped
public class NovaProcessedEvents implements ProcessedEvents {

    /** El consumidor, como lo guarda el inbox. */
    static final String CONSUMER = "catalog-best-sellers";

    private final Inbox inbox;

    /**
     * Crea el adaptador.
     *
     * @param dataSource el {@code DataSource} del servicio
     */
    public NovaProcessedEvents(DataSource dataSource) {
        this.inbox = new JdbcInbox(dataSource::getConnection);
    }

    @Override
    public boolean firstTime(String source, String id) {
        return inbox.register(CONSUMER, source, id);
    }
}
