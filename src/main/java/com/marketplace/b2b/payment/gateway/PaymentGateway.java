package com.marketplace.b2b.payment.gateway;

import java.math.BigDecimal;

/**
 * Contrato con la pasarela de pago. Hoy hay una implementación simulada;
 * para una pasarela real (Stripe, Openpay, Conekta...) solo se agrega otra implementación.
 */
public interface PaymentGateway {

    /**
     * @throws PaymentGatewayException ante errores técnicos transitorios (timeout, 5xx) que vale la pena reintentar
     */
    GatewayResponse charge(ChargeCommand command);

    /**
     * @param idempotencyKey la pasarela no cobra dos veces la misma llave (se usa el folio del pedido)
     * @param cardToken      token generado por el SDK de la pasarela en el front (nunca el número de tarjeta)
     */
    record ChargeCommand(String idempotencyKey, BigDecimal amount, String currency, String cardToken) {
    }

    enum GatewayStatus {
        APPROVED,
        DECLINED
    }

    record GatewayResponse(GatewayStatus status, String reference, String message) {

        public static GatewayResponse approved(String reference) {
            return new GatewayResponse(GatewayStatus.APPROVED, reference, "Aprobado");
        }

        public static GatewayResponse declined(String message) {
            return new GatewayResponse(GatewayStatus.DECLINED, null, message);
        }
    }
}