package com.marketplace.b2b.integration.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos que el ERP necesita para generar la orden de venta (y el CRM para registrar la venta).
 */
public record OrderPaidPayload(
        String orderNumber,
        Long userId,
        String customerEmail,
        String companyName,
        String taxId,
        BigDecimal total,
        String paymentReference,
        LocalDate estimatedDeliveryDate,
        List<Line> lines
) {

    public record Line(String sku, String productName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}