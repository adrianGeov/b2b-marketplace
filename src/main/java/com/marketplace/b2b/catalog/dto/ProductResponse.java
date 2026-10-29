package com.marketplace.b2b.catalog.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        Integer stock,
        boolean active
) {
}