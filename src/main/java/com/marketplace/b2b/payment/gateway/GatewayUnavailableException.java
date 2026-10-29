package com.marketplace.b2b.payment.gateway;

import lombok.Getter;

/**
 * Se agotaron los reintentos contra la pasarela.
 */
@Getter
public class GatewayUnavailableException extends RuntimeException {

    private final int attempts;

    public GatewayUnavailableException(int attempts, Throwable cause) {
        super("Pasarela no disponible tras " + attempts + " intento(s)", cause);
        this.attempts = attempts;
    }
}