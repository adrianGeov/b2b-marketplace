package com.marketplace.b2b.integration.event;

import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.integration.outbox.service.OutboxService;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.payment.entity.Payment;
import com.marketplace.b2b.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Traduce hechos del negocio a eventos de integración. Los módulos de negocio
 * no saben nada de ERP/CRM: solo avisan "se registró un cliente" o "se pagó un pedido".
 */
@Component
@RequiredArgsConstructor
public class IntegrationEventPublisher {

    private final OutboxService outboxService;

    public void customerRegistered(User user) {
        CustomerRegisteredPayload payload = new CustomerRegisteredPayload(
                user.getId(), user.getEmail(), user.getFullName(), user.getCompanyName(),
                user.getTaxId(), user.getRole().name());

        outboxService.enqueue(IntegrationEventTypes.AGGREGATE_USER, String.valueOf(user.getId()),
                OutboxTarget.CRM, IntegrationEventTypes.CUSTOMER_REGISTERED, payload);
    }

    public void orderPaid(PurchaseOrder order, Payment payment) {
        User user = order.getUser();
        List<OrderPaidPayload.Line> lines = order.getItems().stream()
                .map(item -> new OrderPaidPayload.Line(item.getSku(), item.getProductName(), item.getQuantity(),
                        item.getUnitPrice(), item.getSubtotal()))
                .toList();

        OrderPaidPayload payload = new OrderPaidPayload(
                order.getOrderNumber(), user.getId(), user.getEmail(), user.getCompanyName(), user.getTaxId(),
                order.getTotalAmount(), payment.getProviderReference(), order.getEstimatedDeliveryDate(), lines);

        outboxService.enqueue(IntegrationEventTypes.AGGREGATE_ORDER, order.getOrderNumber(),
                OutboxTarget.ERP, IntegrationEventTypes.ORDER_PAID, payload);
        outboxService.enqueue(IntegrationEventTypes.AGGREGATE_ORDER, order.getOrderNumber(),
                OutboxTarget.CRM, IntegrationEventTypes.ORDER_PAID, payload);
    }
}