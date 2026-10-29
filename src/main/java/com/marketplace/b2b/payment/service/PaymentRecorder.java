package com.marketplace.b2b.payment.service;

import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.repository.OrderRepository;
import com.marketplace.b2b.payment.entity.Payment;
import com.marketplace.b2b.payment.entity.PaymentStatus;
import com.marketplace.b2b.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra el intento de pago y actualiza el estatus del pedido en UNA transacción corta.
 * La llamada a la pasarela ocurre FUERA de esta transacción para no retener conexiones
 * de BD mientras se espera a un sistema externo (clave con 3,000 usuarios concurrentes).
 */
@Service
@RequiredArgsConstructor
public class PaymentRecorder {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment record(Long orderId, PaymentAttempt attempt) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderId));

        if (attempt.status() == PaymentStatus.APPROVED) {
            order.setStatus(OrderStatus.PAID);
        } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(attempt.amount())
                .status(attempt.status())
                .providerReference(attempt.providerReference())
                .failureReason(attempt.failureReason())
                .attempts(attempt.attempts())
                .build();

        // saveAndFlush: si ya existe un pago APPROVED, el índice único parcial falla aquí
        return paymentRepository.saveAndFlush(payment);
    }
}