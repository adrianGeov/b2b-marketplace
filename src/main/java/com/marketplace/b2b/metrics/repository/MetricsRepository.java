package com.marketplace.b2b.metrics.repository;

import com.marketplace.b2b.metrics.dto.MetricsResponse.ProductCombination;
import com.marketplace.b2b.metrics.dto.MetricsResponse.TopProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consultas analíticas con SQL directo (JdbcClient).
 * JPA se usa para transacciones; para agregaciones, SQL es más claro y eficiente.
 * Rango de fechas: [from, to) sobre la fecha de creación.
 */
@Repository
@RequiredArgsConstructor
public class MetricsRepository {

    /** Estatus que cuentan como venta concretada. */
    private static final String PAID_STATUSES = "('PAID', 'CONFIRMED', 'SHIPPED', 'DELIVERED')";

    private final JdbcClient jdbcClient;

    public SalesSummary salesSummary(OffsetDateTime from, OffsetDateTime to) {
        return jdbcClient.sql("""
                        SELECT COUNT(*)                AS paid_orders,
                               COALESCE(SUM(total), 0) AS revenue,
                               COALESCE(AVG(total), 0) AS average_ticket
                        FROM pedidos
                        WHERE estatus IN %s
                          AND fecha_creacion >= :from AND fecha_creacion < :to
                        """.formatted(PAID_STATUSES))
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new SalesSummary(
                        rs.getLong("paid_orders"), rs.getBigDecimal("revenue"), rs.getBigDecimal("average_ticket")))
                .single();
    }

    public ConversionCounts conversionCounts(OffsetDateTime from, OffsetDateTime to) {
        return jdbcClient.sql("""
                        SELECT
                          (SELECT COUNT(*) FROM usuarios u
                            WHERE u.rol <> 'ADMIN'
                              AND u.fecha_creacion >= :from AND u.fecha_creacion < :to) AS leads,
                          (SELECT COUNT(DISTINCT u.id) FROM usuarios u
                             JOIN pedidos p ON p.usuario_id = u.id
                            WHERE u.rol <> 'ADMIN'
                              AND u.fecha_creacion >= :from AND u.fecha_creacion < :to
                              AND p.estatus IN %s) AS buyers,
                          (SELECT COUNT(*) FROM pedidos p
                            WHERE p.fecha_creacion >= :from AND p.fecha_creacion < :to) AS orders_created
                        """.formatted(PAID_STATUSES))
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new ConversionCounts(
                        rs.getLong("leads"), rs.getLong("buyers"), rs.getLong("orders_created")))
                .single();
    }

    public DeliveryCounts deliveryCounts(OffsetDateTime from, OffsetDateTime to) {
        return jdbcClient.sql("""
                        SELECT COUNT(*) AS delivered,
                               COUNT(*) FILTER (
                                 WHERE (fecha_entrega AT TIME ZONE 'UTC')::date <= fecha_entrega_estimada
                               ) AS on_time
                        FROM pedidos
                        WHERE estatus = 'DELIVERED' AND fecha_entrega IS NOT NULL
                          AND fecha_creacion >= :from AND fecha_creacion < :to
                        """)
                .param("from", from)
                .param("to", to)
                .query((rs, rowNum) -> new DeliveryCounts(rs.getLong("delivered"), rs.getLong("on_time")))
                .single();
    }

    public List<TopProduct> topProducts(OffsetDateTime from, OffsetDateTime to, int limit) {
        return jdbcClient.sql("""
                        SELECT MAX(d.sku)             AS sku,
                               MAX(d.nombre_producto) AS name,
                               SUM(d.cantidad)        AS units_sold,
                               SUM(d.subtotal)        AS revenue
                        FROM pedido_detalles d
                        JOIN pedidos p ON p.id = d.pedido_id
                        WHERE p.estatus IN %s
                          AND p.fecha_creacion >= :from AND p.fecha_creacion < :to
                        GROUP BY d.producto_id
                        ORDER BY units_sold DESC, revenue DESC
                        LIMIT :limit
                        """.formatted(PAID_STATUSES))
                .param("from", from)
                .param("to", to)
                .param("limit", limit)
                .query((rs, rowNum) -> new TopProduct(rs.getString("sku"), rs.getString("name"),
                        rs.getLong("units_sold"), rs.getBigDecimal("revenue")))
                .list();
    }

    /**
     * Pares de productos comprados en el mismo pedido (self-join del detalle).
     * a.producto_id &lt; b.producto_id evita contar (A,B) y (B,A) como pares distintos.
     */
    public List<ProductCombination> topProductPairs(OffsetDateTime from, OffsetDateTime to, int limit) {
        return jdbcClient.sql("""
                        SELECT MAX(a.sku) AS sku_a, MAX(a.nombre_producto) AS name_a,
                               MAX(b.sku) AS sku_b, MAX(b.nombre_producto) AS name_b,
                               COUNT(DISTINCT a.pedido_id) AS orders_together
                        FROM pedido_detalles a
                        JOIN pedido_detalles b ON b.pedido_id = a.pedido_id AND a.producto_id < b.producto_id
                        JOIN pedidos p ON p.id = a.pedido_id
                        WHERE p.estatus IN %s
                          AND p.fecha_creacion >= :from AND p.fecha_creacion < :to
                        GROUP BY a.producto_id, b.producto_id
                        ORDER BY orders_together DESC, sku_a
                        LIMIT :limit
                        """.formatted(PAID_STATUSES))
                .param("from", from)
                .param("to", to)
                .param("limit", limit)
                .query((rs, rowNum) -> new ProductCombination(rs.getString("sku_a"), rs.getString("name_a"),
                        rs.getString("sku_b"), rs.getString("name_b"), rs.getLong("orders_together")))
                .list();
    }

    // ------------------------------------------------------------------
    // Proyecciones internas
    // ------------------------------------------------------------------

    public record SalesSummary(long paidOrders, BigDecimal revenue, BigDecimal averageTicket) {
    }

    public record ConversionCounts(long leads, long buyers, long ordersCreated) {
    }

    public record DeliveryCounts(long delivered, long onTime) {
    }
}