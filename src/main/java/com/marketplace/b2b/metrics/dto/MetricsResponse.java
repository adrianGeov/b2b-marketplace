package com.marketplace.b2b.metrics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores de negocio del eCommerce para el periodo consultado.
 * Los porcentajes van de 0 a 100 con 2 decimales.
 */
public record MetricsResponse(
        Period period,
        Sales sales,
        Conversion conversion,
        Deliveries deliveries,
        List<TopProduct> topProducts,
        List<ProductCombination> topCombinations,
        List<String> notes
) {

    public record Period(LocalDate from, LocalDate to) {
    }

    /** Solo pedidos pagados (PAID, CONFIRMED, SHIPPED, DELIVERED). */
    public record Sales(long paidOrders, BigDecimal revenue, BigDecimal averageTicket) {
    }

    /**
     * @param leads                  clientes registrados en el periodo
     * @param buyers                 de esos leads, cuántos tienen al menos un pedido pagado
     * @param leadConversionRate     buyers / leads
     * @param ordersCreated          pedidos creados en el periodo (cualquier estatus)
     * @param checkoutConversionRate pedidos pagados / pedidos creados
     */
    public record Conversion(long leads, long buyers, BigDecimal leadConversionRate,
                             long ordersCreated, long ordersPaid, BigDecimal checkoutConversionRate) {
    }

    public record Deliveries(long delivered, long onTime, long late, BigDecimal onTimeRate) {
    }

    public record TopProduct(String sku, String name, long unitsSold, BigDecimal revenue) {
    }

    /** Dos productos que se compran juntos en el mismo pedido. */
    public record ProductCombination(String skuA, String nameA, String skuB, String nameB, long ordersTogether) {
    }
}