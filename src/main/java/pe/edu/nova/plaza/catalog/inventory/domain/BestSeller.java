package pe.edu.nova.plaza.catalog.inventory.domain;

/**
 * Un producto del ranking de lo más vendido.
 *
 * @param sku       el código
 * @param name      el nombre
 * @param unitsSold las unidades vendidas en pedidos confirmados
 */
public record BestSeller(String sku, String name, long unitsSold) {}
