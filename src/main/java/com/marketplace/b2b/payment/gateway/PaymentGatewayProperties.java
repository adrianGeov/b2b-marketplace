package com.marketplace.b2b.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propiedades app.payment.gateway.*
 *
 * @param failureRate       probabilidad de error transitorio por intento (solo pasarela simulada)
 * @param maxAttempts       intentos totales (1 = sin reintentos)
 * @param initialBackoff    espera antes del primer reintento
 * @param backoffMultiplier factor de crecimiento de la espera (backoff exponencial)
 */
@ConfigurationProperties(prefix = "app.payment.gateway")
public record PaymentGatewayProperties(double failureRate, int maxAttempts, Duration initialBackoff,
                                       double backoffMultiplier) {

    public PaymentGatewayProperties {
        failureRate = Math.max(0.0, Math.min(1.0, failureRate));
        if (maxAttempts <= 0) {
            maxAttempts = 3;
        }
        if (initialBackoff == null || initialBackoff.isNegative()) {
            initialBackoff = Duration.ofMillis(200);
        }
        if (backoffMultiplier < 1.0) {
            backoffMultiplier = 2.0;
        }
    }
}