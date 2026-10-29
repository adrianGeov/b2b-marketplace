package com.marketplace.b2b.cart.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @param totalItems suma de las cantidades (no el número de líneas)
 */
public record CartResponse(
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal total
) {

    public static CartResponse of(List<CartItemResponse> items) {
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new CartResponse(List.copyOf(items), totalItems, total);
    }

    public static CartResponse empty() {
        return of(List.of());
    }
}