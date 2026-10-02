package pe.edu.nova.plaza.catalog.inventory.port.in;

import java.math.BigDecimal;

/**
 * Un producto como lo ve quien compra: su precio y cuánto se puede reservar ahora.
 *
 * @param sku       el código del producto
 * @param name      el nombre
 * @param price     el precio de una unidad
 * @param currency  la moneda, en ISO 4217
 * @param available las unidades que se pueden reservar
 */
public record ProductView(String sku, String name, BigDecimal price, String currency, int available) {}
