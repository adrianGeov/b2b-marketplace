package com.marketplace.b2b.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {
}