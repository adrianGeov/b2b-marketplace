package com.marketplace.b2b.payment.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pasarela simulada para el MVP. Reproduce los comportamientos del caso de uso:
 * - tok_declined      → rechazo (fondos insuficientes)
 * - tok_gateway_down  → caída total (siempre error técnico)
 * - cualquier otro    → aprobado, con errores intermitentes según app.payment.gateway.failure-rate
 * Igual que una pasarela real, no cobra dos veces la misma llave de idempotencia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedPaymentGateway implements PaymentGateway {

    public static final String DECLINED_TOKEN = "tok_declined";
    public static final String UNAVAILABLE_TOKEN = "tok_gateway_down";

    private final PaymentGatewayProperties properties;

    /** Cargos aprobados por llave de idempotencia (en una pasarela real esto vive en su servidor). */
    private final Map<String, GatewayResponse> approvedCharges = new ConcurrentHashMap<>();

    @Override
    public GatewayResponse charge(ChargeCommand command) {
        GatewayResponse previous = approvedCharges.get(command.idempotencyKey());
        if (previous != null) {
            log.info("Pasarela: la llave {} ya tenía un cargo aprobado; no se cobra de nuevo", command.idempotencyKey());
            return previous;
        }

        if (UNAVAILABLE_TOKEN.equals(command.cardToken())) {
            throw new PaymentGatewayException("Servicio no disponible (503 simulado)");
        }
        if (ThreadLocalRandom.current().nextDouble() < properties.failureRate()) {
            throw new PaymentGatewayException("Timeout simulado de la pasarela");
        }
        if (DECLINED_TOKEN.equals(command.cardToken())) {
            return GatewayResponse.declined("Fondos insuficientes");
        }

        return approvedCharges.computeIfAbsent(command.idempotencyKey(),
                key -> GatewayResponse.approved("PAY-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT)));
    }
}