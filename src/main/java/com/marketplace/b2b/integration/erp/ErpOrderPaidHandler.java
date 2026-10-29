package com.marketplace.b2b.integration.erp;

import com.marketplace.b2b.integration.IntegrationException;
import com.marketplace.b2b.integration.event.IntegrationEventTypes;
import com.marketplace.b2b.integration.event.OrderPaidPayload;
import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.integration.outbox.service.OutboxEventHandler;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pedido pagado ->  crear orden de venta en el ERP -> pedido CONFIRMED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpOrderPaidHandler implements OutboxEventHandler {

    private final ErpClient erpClient;
    private final OrderRepository orderRepository;
    private final JsonMapper jsonMapper;

    @Override
    public boolean supports(OutboxEvent event) {
        return event.getTarget() == OutboxTarget.ERP
                && IntegrationEventTypes.ORDER_PAID.equals(event.getEventType());
    }

    @Override
    public void handle(OutboxEvent event) {
        OrderPaidPayload payload = jsonMapper.readValue(event.getPayload(), OrderPaidPayload.class);

        String erpOrderId = erpClient.createSalesOrder(payload);

        PurchaseOrder order = orderRepository.findByOrderNumber(payload.orderNumber())
                .orElseThrow(() -> new IntegrationException("Pedido " + payload.orderNumber() + " no encontrado"));
        order.setErpOrderId(erpOrderId);
        if (order.getStatus() == OrderStatus.PAID) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        log.info("Pedido {} confirmado por el ERP con id {}", order.getOrderNumber(), erpOrderId);
    }
}