package com.marketplace.b2b.order.dto;

import com.marketplace.b2b.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        String orderNumber,
        OrderStatus status,
        BigDecimal total,
        LocalDate estimatedDeliveryDate,
        Instant deliveredAt,
        String erpOrderId,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}