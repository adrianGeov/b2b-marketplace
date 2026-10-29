package com.marketplace.b2b.payment.gateway;

/**
 * Error técnico transitorio de la pasarela (timeout, 503...). Se puede reintentar.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }
}