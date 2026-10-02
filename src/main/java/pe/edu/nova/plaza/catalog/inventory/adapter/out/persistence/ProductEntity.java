package pe.edu.nova.plaza.catalog.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import pe.edu.nova.plaza.catalog.inventory.domain.Product;

/** La fila de un producto. El dominio no la ve: el adaptador la convierte en {@link Product}. */
@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @Column(length = 64)
    String sku;

    @Column(nullable = false, length = 200)
    String name;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal price;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false)
    int stock;

    @Version
    long version;

    /** Para JPA. */
    protected ProductEntity() {}

    Product toDomain() {
        return new Product(sku, name, price, currency, stock);
    }
}
