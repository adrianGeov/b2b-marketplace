package com.marketplace.b2b.payment.gateway;

import com.marketplace.b2b.payment.gateway.PaymentGateway.ChargeCommand;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayResponse;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayStatus;
import com.marketplace.b2b.payment.gateway.PaymentGatewayClient.ChargeOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayClientTest {

    private static final ChargeCommand COMMAND =
            new ChargeCommand("PED-20261028-AAAAAA", new BigDecimal("2665.00"), "MXN", "tok_visa");

    @Mock
    private PaymentGateway gateway;

    private PaymentGatewayClient client;

    @BeforeEach
    void setUp() {
        // Backoff en cero para que las pruebas no esperen
        client = new PaymentGatewayClient(gateway, new PaymentGatewayProperties(0.0, 3, Duration.ZERO, 2.0));
    }

    @Test
    @DisplayName("Reintenta ante errores transitorios y devuelve la respuesta cuando la pasarela se recupera")
    void shouldRetryUntilSuccess() {
        given(gateway.charge(COMMAND))
                .willThrow(new PaymentGatewayException("timeout 1"), new PaymentGatewayException("timeout 2"))
                .willReturn(GatewayResponse.approved("PAY-123"));

        ChargeOutcome outcome = client.charge(COMMAND);

        assertThat(outcome.response().status()).isEqualTo(GatewayStatus.APPROVED);
        assertThat(outcome.attempts()).isEqualTo(3);
        verify(gateway, times(3)).charge(COMMAND);
    }

    @Test
    @DisplayName("Al agotar los intentos lanza GatewayUnavailableException con el número de intentos")
    void shouldGiveUpAfterMaxAttempts() {
        given(gateway.charge(COMMAND)).willThrow(new PaymentGatewayException("503"));

        assertThatThrownBy(() -> client.charge(COMMAND))
                .isInstanceOf(GatewayUnavailableException.class)
                .extracting("attempts")
                .isEqualTo(3);

        verify(gateway, times(3)).charge(COMMAND);
    }

    @Test
    @DisplayName("Un rechazo NO se reintenta (es una respuesta de negocio, no un error técnico)")
    void shouldNotRetryDeclinedPayment() {
        given(gateway.charge(COMMAND)).willReturn(GatewayResponse.declined("Fondos insuficientes"));

        ChargeOutcome outcome = client.charge(COMMAND);

        assertThat(outcome.response().status()).isEqualTo(GatewayStatus.DECLINED);
        assertThat(outcome.attempts()).isEqualTo(1);
        verify(gateway, times(1)).charge(COMMAND);
    }
}