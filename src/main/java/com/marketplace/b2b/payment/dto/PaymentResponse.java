package com.marketplace.b2b.payment.dto;

import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        String orderNumber,
        PaymentStatus status,
        BigDecimal amount,
        String providerReference,
        String failureReason,
        Integer attempts,
        Instant createdAt,
        OrderStatus orderStatus
) {
}