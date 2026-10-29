package com.marketplace.b2b.payment.service;

import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.repository.OrderRepository;
import com.marketplace.b2b.payment.dto.PaymentRequest;
import com.marketplace.b2b.payment.dto.PaymentResponse;
import com.marketplace.b2b.payment.entity.Payment;
import com.marketplace.b2b.payment.entity.PaymentStatus;
import com.marketplace.b2b.payment.gateway.GatewayUnavailableException;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayResponse;
import com.marketplace.b2b.payment.gateway.PaymentGatewayClient;
import com.marketplace.b2b.payment.gateway.PaymentGatewayClient.ChargeOutcome;
import com.marketplace.b2b.payment.gateway.PaymentGatewayException;
import com.marketplace.b2b.payment.mapper.PaymentMapper;
import com.marketplace.b2b.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 50L;
    private static final String FOLIO = "PED-20261028-AAAAAA";
    private static final PaymentRequest REQUEST = new PaymentRequest("tok_visa");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentGatewayClient gatewayClient;
    @Mock
    private PaymentRecorder paymentRecorder;
    @Mock
    private PaymentRepository paymentRepository;

    @Spy
    private PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Pago aprobado: registra APPROVED con los intentos usados y el pedido queda PAID")
    void shouldApprovePayment() {
        PurchaseOrder order = order(OrderStatus.PENDING_PAYMENT);
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID)).willReturn(Optional.of(order));
        given(gatewayClient.charge(any())).willReturn(new ChargeOutcome(GatewayResponse.approved("PAY-1"), 2));
        given(paymentRecorder.record(eq(ORDER_ID), any(PaymentAttempt.class)))
                .willReturn(payment(order(OrderStatus.PAID), PaymentStatus.APPROVED, "PAY-1", 2));

        PaymentResponse response = paymentService.pay(USER_ID, FOLIO, REQUEST);

        ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentRecorder).record(eq(ORDER_ID), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(captor.getValue().providerReference()).isEqualTo("PAY-1");
        assertThat(captor.getValue().attempts()).isEqualTo(2);

        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.orderNumber()).isEqualTo(FOLIO);
    }

    @Test
    @DisplayName("Pago rechazado: registra DECLINED y responde PAYMENT_DECLINED")
    void shouldRegisterDeclinedPayment() {
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID))
                .willReturn(Optional.of(order(OrderStatus.PENDING_PAYMENT)));
        given(gatewayClient.charge(any()))
                .willReturn(new ChargeOutcome(GatewayResponse.declined("Fondos insuficientes"), 1));

        assertThatThrownBy(() -> paymentService.pay(USER_ID, FOLIO, REQUEST))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_DECLINED);

        ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentRecorder).record(eq(ORDER_ID), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(captor.getValue().failureReason()).isEqualTo("Fondos insuficientes");
    }

    @Test
    @DisplayName("Pasarela caída: registra FAILED con 3 intentos y responde 503")
    void shouldRegisterFailedPaymentWhenGatewayIsDown() {
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID))
                .willReturn(Optional.of(order(OrderStatus.PENDING_PAYMENT)));
        given(gatewayClient.charge(any()))
                .willThrow(new GatewayUnavailableException(3, new PaymentGatewayException("503")));

        assertThatThrownBy(() -> paymentService.pay(USER_ID, FOLIO, REQUEST))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);

        ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentRecorder).record(eq(ORDER_ID), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(captor.getValue().attempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("Un pedido ya pagado no se vuelve a cobrar (ni siquiera se llama a la pasarela)")
    void shouldNotChargePaidOrderAgain() {
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID))
                .willReturn(Optional.of(order(OrderStatus.PAID)));

        assertThatThrownBy(() -> paymentService.pay(USER_ID, FOLIO, REQUEST))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ORDER_STATE);

        verifyNoInteractions(gatewayClient, paymentRecorder);
    }

    @Test
    @DisplayName("Pago concurrente del mismo pedido: devuelve el pago aprobado que ganó")
    void shouldReturnExistingApprovedPaymentOnConcurrentDuplicate() {
        PurchaseOrder order = order(OrderStatus.PENDING_PAYMENT);
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID)).willReturn(Optional.of(order));
        given(gatewayClient.charge(any())).willReturn(new ChargeOutcome(GatewayResponse.approved("PAY-1"), 1));
        given(paymentRecorder.record(eq(ORDER_ID), any(PaymentAttempt.class)))
                .willThrow(new DataIntegrityViolationException("ux_pagos_pedido_aprobado"));
        given(paymentRepository.findFirstByOrderIdAndStatus(ORDER_ID, PaymentStatus.APPROVED))
                .willReturn(Optional.of(payment(order(OrderStatus.PAID), PaymentStatus.APPROVED, "PAY-1", 1)));

        PaymentResponse response = paymentService.pay(USER_ID, FOLIO, REQUEST);

        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.providerReference()).isEqualTo("PAY-1");
    }

    // ------------------------------------------------------------------

    private static PurchaseOrder order(OrderStatus status) {
        return PurchaseOrder.builder()
                .id(ORDER_ID)
                .orderNumber(FOLIO)
                .status(status)
                .idempotencyKey("pedido-ana-0001")
                .totalAmount(new BigDecimal("2665.00"))
                .build();
    }

    private static Payment payment(PurchaseOrder order, PaymentStatus status, String reference, int attempts) {
        return Payment.builder()
                .id(900L)
                .order(order)
                .amount(order.getTotalAmount())
                .status(status)
                .providerReference(reference)
                .attempts(attempts)
                .build();
    }
}