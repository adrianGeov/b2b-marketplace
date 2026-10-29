package com.marketplace.b2b.integration.event;

import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.integration.outbox.service.OutboxService;
import com.marketplace.b2b.order.entity.OrderItem;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.payment.entity.Payment;
import com.marketplace.b2b.payment.entity.PaymentStatus;
import com.marketplace.b2b.user.entity.Role;
import com.marketplace.b2b.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IntegrationEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private IntegrationEventPublisher publisher;

    @Test
    @DisplayName("Un cliente registrado genera un evento CUSTOMER_REGISTERED hacia el CRM")
    void shouldPublishCustomerRegistered() {
        publisher.customerRegistered(user());

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).enqueue(eq(IntegrationEventTypes.AGGREGATE_USER), eq("10"), eq(OutboxTarget.CRM),
                eq(IntegrationEventTypes.CUSTOMER_REGISTERED), payload.capture());

        assertThat(payload.getValue()).isInstanceOfSatisfying(CustomerRegisteredPayload.class, p -> {
            assertThat(p.email()).isEqualTo("compras@acme.mx");
            assertThat(p.role()).isEqualTo("DISTRIBUTOR");
        });
    }

    @Test
    @DisplayName("Un pedido pagado genera un evento ORDER_PAID hacia el ERP y otro hacia el CRM")
    void shouldPublishOrderPaidToErpAndCrm() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(50L)
                .orderNumber("PED-20261028-AAAAAA")
                .user(user())
                .status(OrderStatus.PAID)
                .idempotencyKey("pedido-ana-0001")
                .totalAmount(new BigDecimal("1798.00"))
                .build();
        order.addItem(OrderItem.builder().sku("PAP-001").productName("Hojas").quantity(2)
                .unitPrice(new BigDecimal("899.00")).subtotal(new BigDecimal("1798.00")).build());
        Payment payment = Payment.builder().id(900L).order(order).status(PaymentStatus.APPROVED)
                .amount(new BigDecimal("1798.00")).providerReference("PAY-123").build();

        publisher.orderPaid(order, payment);

        ArgumentCaptor<Object> erpPayload = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).enqueue(eq(IntegrationEventTypes.AGGREGATE_ORDER), eq("PED-20261028-AAAAAA"),
                eq(OutboxTarget.ERP), eq(IntegrationEventTypes.ORDER_PAID), erpPayload.capture());
        verify(outboxService).enqueue(eq(IntegrationEventTypes.AGGREGATE_ORDER), eq("PED-20261028-AAAAAA"),
                eq(OutboxTarget.CRM), eq(IntegrationEventTypes.ORDER_PAID), eq(erpPayload.getValue()));

        assertThat(erpPayload.getValue()).isInstanceOfSatisfying(OrderPaidPayload.class, p -> {
            assertThat(p.paymentReference()).isEqualTo("PAY-123");
            assertThat(p.total()).isEqualByComparingTo("1798.00");
            assertThat(p.lines()).hasSize(1);
            assertThat(p.lines().get(0).sku()).isEqualTo("PAP-001");
        });
    }

    private static User user() {
        return User.builder()
                .id(10L).email("compras@acme.mx").fullName("Ana Lopez").companyName("Acme SA de CV")
                .taxId("AAA010101AAA").role(Role.DISTRIBUTOR)
                .build();
    }
}