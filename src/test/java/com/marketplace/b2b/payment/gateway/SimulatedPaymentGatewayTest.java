package com.marketplace.b2b.payment.gateway;

import com.marketplace.b2b.payment.gateway.PaymentGateway.ChargeCommand;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayResponse;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedPaymentGatewayTest {

    /** failureRate = 0 para que las pruebas sean deterministas. */
    private final SimulatedPaymentGateway gateway =
            new SimulatedPaymentGateway(new PaymentGatewayProperties(0.0, 3, Duration.ZERO, 2.0));

    @Test
    @DisplayName("Un token válido se aprueba con referencia PAY-...")
    void shouldApproveValidToken() {
        GatewayResponse response = gateway.charge(command("PED-1", "tok_visa"));

        assertThat(response.status()).isEqualTo(GatewayStatus.APPROVED);
        assertThat(response.reference()).startsWith("PAY-");
    }

    @Test
    @DisplayName("tok_declined se rechaza por fondos insuficientes")
    void shouldDeclineDeclinedToken() {
        GatewayResponse response = gateway.charge(command("PED-2", SimulatedPaymentGateway.DECLINED_TOKEN));

        assertThat(response.status()).isEqualTo(GatewayStatus.DECLINED);
        assertThat(response.message()).isEqualTo("Fondos insuficientes");
    }

    @Test
    @DisplayName("tok_gateway_down simula una caída total de la pasarela")
    void shouldFailWhenGatewayIsDown() {
        assertThatThrownBy(() -> gateway.charge(command("PED-3", SimulatedPaymentGateway.UNAVAILABLE_TOKEN)))
                .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    @DisplayName("La misma llave de idempotencia NO genera un segundo cargo")
    void shouldNotChargeTwiceForSameKey() {
        GatewayResponse first = gateway.charge(command("PED-4", "tok_visa"));
        GatewayResponse second = gateway.charge(command("PED-4", "tok_visa"));

        assertThat(second.reference()).isEqualTo(first.reference());
    }

    private static ChargeCommand command(String key, String token) {
        return new ChargeCommand(key, new BigDecimal("100.00"), "MXN", token);
    }
}