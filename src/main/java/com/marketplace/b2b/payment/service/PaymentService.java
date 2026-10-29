package com.marketplace.b2b.payment.service;

import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.repository.OrderRepository;
import com.marketplace.b2b.payment.dto.PaymentRequest;
import com.marketplace.b2b.payment.dto.PaymentResponse;
import com.marketplace.b2b.payment.entity.Payment;
import com.marketplace.b2b.payment.entity.PaymentStatus;
import com.marketplace.b2b.payment.gateway.GatewayUnavailableException;
import com.marketplace.b2b.payment.gateway.PaymentGateway.ChargeCommand;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayResponse;
import com.marketplace.b2b.payment.gateway.PaymentGateway.GatewayStatus;
import com.marketplace.b2b.payment.gateway.PaymentGatewayClient;
import com.marketplace.b2b.payment.gateway.PaymentGatewayClient.ChargeOutcome;
import com.marketplace.b2b.payment.mapper.PaymentMapper;
import com.marketplace.b2b.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CURRENCY = "MXN";
    private static final Set<OrderStatus> PAYABLE = EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED);

    private final OrderRepository orderRepository;
    private final PaymentGatewayClient gatewayClient;
    private final PaymentRecorder paymentRecorder;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    /**
     * Flujo: validar pedido → cobrar (con reintentos, fuera de transacción) → registrar resultado.
     */
    public PaymentResponse pay(Long userId, String orderNumber, PaymentRequest request) {
        PurchaseOrder order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber));
        ensurePayable(order);

        BigDecimal amount = order.getTotalAmount();
        ChargeCommand command = new ChargeCommand(order.getOrderNumber(), amount, CURRENCY, request.cardToken());

        ChargeOutcome outcome;
        try {
            outcome = gatewayClient.charge(command);
        } catch (GatewayUnavailableException ex) {
            paymentRecorder.record(order.getId(), PaymentAttempt.failed(amount, ex.getMessage(), ex.getAttempts()));
            log.error("Pago no procesado: pasarela no disponible pedido={} intentos={}", orderNumber, ex.getAttempts());
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE,
                    "La pasarela de pago no está disponible. Su pedido sigue reservado; intente pagar en unos minutos");
        }

        GatewayResponse response = outcome.response();
        if (response.status() == GatewayStatus.DECLINED) {
            paymentRecorder.record(order.getId(),
                    PaymentAttempt.declined(amount, response.message(), outcome.attempts()));
            log.info("Pago rechazado pedido={} motivo={}", orderNumber, response.message());
            throw new BusinessException(ErrorCode.PAYMENT_DECLINED,
                    "Pago rechazado: " + response.message() + ". Intente con otro método de pago");
        }

        return recordApproved(order, amount, response.reference(), outcome.attempts());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(Long userId, String orderNumber, boolean isAdmin) {
        Optional<PurchaseOrder> order = isAdmin
                ? orderRepository.findByOrderNumber(orderNumber)
                : orderRepository.findByOrderNumberAndUserId(orderNumber, userId);
        Long orderId = order.map(PurchaseOrder::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber));
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------

    private PaymentResponse recordApproved(PurchaseOrder order, BigDecimal amount, String reference, int attempts) {
        try {
            Payment payment = paymentRecorder.record(order.getId(), PaymentAttempt.approved(amount, reference, attempts));
            log.info("Pago aprobado pedido={} referencia={} intentos={}", order.getOrderNumber(), reference, attempts);
            return paymentMapper.toResponse(payment);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException ex) {
            // Dos pagos simultáneos del mismo pedido: la pasarela cobró una sola vez (misma llave)
            // y la BD solo admite un APPROVED. Se devuelve el pago que ganó.
            log.warn("Pago concurrente detectado pedido={}; se devuelve el pago aprobado existente",
                    order.getOrderNumber());
            return paymentRepository.findFirstByOrderIdAndStatus(order.getId(), PaymentStatus.APPROVED)
                    .map(paymentMapper::toResponse)
                    .orElseThrow(() -> (DataAccessException) ex);
        }
    }

    private static void ensurePayable(PurchaseOrder order) {
        if (PAYABLE.contains(order.getStatus())) {
            return;
        }
        String reason = order.getStatus() == OrderStatus.CANCELLED ? "está cancelado" : "ya está pagado";
        throw new BusinessException(ErrorCode.INVALID_ORDER_STATE,
                "El pedido " + order.getOrderNumber() + " " + reason + " (estatus " + order.getStatus() + ")");
    }
}