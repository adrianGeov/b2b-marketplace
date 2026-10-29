package com.marketplace.b2b.order.dto;

import com.marketplace.b2b.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Versión ligera para listados (sin detalle de productos, evita consultas N+1).
 */
public record OrderSummaryResponse(
        String orderNumber,
        OrderStatus status,
        BigDecimal total,
        LocalDate estimatedDeliveryDate,
        Instant createdAt
) {
}