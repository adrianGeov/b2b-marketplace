package com.marketplace.b2b.payment.service;

import com.marketplace.b2b.payment.entity.PaymentStatus;

import java.math.BigDecimal;

/**
 * Resultado de un intento de cobro, listo para registrarse.
 */
public record PaymentAttempt(PaymentStatus status, BigDecimal amount, String providerReference,
                             String failureReason, int attempts) {

    public static PaymentAttempt approved(BigDecimal amount, String reference, int attempts) {
        return new PaymentAttempt(PaymentStatus.APPROVED, amount, reference, null, attempts);
    }

    public static PaymentAttempt declined(BigDecimal amount, String reason, int attempts) {
        return new PaymentAttempt(PaymentStatus.DECLINED, amount, null, reason, attempts);
    }

    public static PaymentAttempt failed(BigDecimal amount, String reason, int attempts) {
        return new PaymentAttempt(PaymentStatus.FAILED, amount, null, reason, attempts);
    }
}