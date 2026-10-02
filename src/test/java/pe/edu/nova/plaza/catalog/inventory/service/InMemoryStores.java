package pe.edu.nova.plaza.catalog.inventory.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import pe.edu.nova.java.libs.persistence.CursorCodec;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;
import pe.edu.nova.plaza.catalog.inventory.domain.Product;
import pe.edu.nova.plaza.catalog.inventory.domain.Reservation;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationLine;
import pe.edu.nova.plaza.catalog.inventory.domain.ReservationStatus;
import pe.edu.nova.plaza.catalog.inventory.port.out.ProductStore;
import pe.edu.nova.plaza.catalog.inventory.port.out.ReservationStore;

/** Los puertos de salida en memoria: los casos de uso se prueban sin base, que es lo que promete el hexágono. */
final class InMemoryStores {

    private InMemoryStores() {}

    /** Los productos, ordenados por código como en la base. */
    static final class Products implements ProductStore {

        final Map<String, Product> rows = new TreeMap<>();
        private final Reservations reservations;

        Products(Reservations reservations, Product... products) {
            this.reservations = reservations;
            for (Product product : products) {
                rows.put(product.sku(), product);
            }
        }

        @Override
        public CursorPage<Product> page(CursorRequest request) {
            String after = request.cursorIfAny()
                    .map(cursor -> (String) CursorCodec.decode(cursor, "sku", List.of("sku")).get("sku"))
                    .orElse("");
            List<Product> rest = rows.values().stream()
                    .filter(product -> product.sku().compareTo(after) > 0)
                    .toList();
            if (rest.size() <= request.limit()) {
                return CursorPage.last(rest);
            }
            List<Product> items = rest.subList(0, request.limit());
            return CursorPage.of(items, CursorCodec.encode("sku", Map.of("sku", items.get(items.size() - 1).sku())));
        }

        @Override
        public Optional<Product> find(String sku) {
            return Optional.ofNullable(rows.get(sku));
        }

        @Override
        public List<Product> lock(Collection<String> skus) {
            return rows.values().stream().filter(product -> skus.contains(product.sku())).toList();
        }

        @Override
        public Map<String, Integer> held(Collection<String> skus, Instant now) {
            Map<String, Integer> held = new HashMap<>();
            for (Reservation reservation : reservations.rows.values()) {
                if (reservation.status() != ReservationStatus.HELD || !now.isBefore(reservation.expiresAt())) {
                    continue;
                }
                for (ReservationLine line : reservation.lines()) {
                    if (skus.contains(line.sku())) {
                        held.merge(line.sku(), line.quantity(), Integer::sum);
                    }
                }
            }
            return held;
        }

        @Override
        public void save(Product product) {
            rows.put(product.sku(), product);
        }
    }

    /** Las reservas. */
    static final class Reservations implements ReservationStore {

        final Map<UUID, Reservation> rows = new HashMap<>();

        @Override
        public Optional<Reservation> lock(UUID id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public void save(Reservation reservation) {
            rows.put(reservation.id(), reservation);
        }
    }

    /** Un reloj que la prueba adelanta. */
    static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(java.time.Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
