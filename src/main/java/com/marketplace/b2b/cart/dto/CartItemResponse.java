package com.marketplace.b2b.cart.dto;

import java.math.BigDecimal;

/**
 * @param available false si el producto se desactivó o ya no hay existencias suficientes
 */
public record CartItemResponse(
        Long productId,
        String sku,
        String name,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Integer availableStock,
        boolean available
) {
}