package com.marketplace.b2b.payment.gateway;

import com.marketplace.b2b.payment.gateway.PaymentGateway.ChargeCommand;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Envuelve la pasarela con reintentos y backoff exponencial con jitter.
 * Solo se reintentan errores técnicos (PaymentGatewayException); un rechazo es una respuesta válida.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayClient {

    private final PaymentGateway gateway;
    private final PaymentGatewayProperties properties;

    public ChargeOutcome charge(ChargeCommand command) {
        int maxAttempts = properties.maxAttempts();
        long backoffMs = properties.initialBackoff().toMillis();
        PaymentGatewayException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                GatewayResponse response = gateway.charge(command);
                if (attempt > 1) {
                    log.info("Pasarela respondió en el intento {}/{} pedido={}", attempt, maxAttempts,
                            command.idempotencyKey());
                }
                return new ChargeOutcome(response, attempt);
            } catch (PaymentGatewayException ex) {
                lastError = ex;
                log.warn("Error transitorio de la pasarela (intento {}/{}) pedido={}: {}",
                        attempt, maxAttempts, command.idempotencyKey(), ex.getMessage());
                if (attempt < maxAttempts && !sleep(withJitter(backoffMs))) {
                    break;
                }
                backoffMs = (long) (backoffMs * properties.backoffMultiplier());
            }
        }
        throw new GatewayUnavailableException(maxAttempts, lastError);
    }

    /** El jitter evita que miles de clientes reintenten exactamente al mismo tiempo. */
    private static long withJitter(long backoffMs) {
        return backoffMs <= 0 ? 0 : backoffMs + ThreadLocalRandom.current().nextLong(backoffMs / 2 + 1);
    }

    private static boolean sleep(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * @param attempts número de intentos que se necesitaron
     */
    public record ChargeOutcome(GatewayResponse response, int attempts) {
    }
}