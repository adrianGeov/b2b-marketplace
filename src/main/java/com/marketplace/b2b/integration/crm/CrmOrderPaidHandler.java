package com.marketplace.b2b.integration.crm;

import com.marketplace.b2b.integration.event.IntegrationEventTypes;
import com.marketplace.b2b.integration.event.OrderPaidPayload;
import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.integration.outbox.service.OutboxEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pedido pagado -> venta ganada en el CRM.
 */
@Component
@RequiredArgsConstructor
public class CrmOrderPaidHandler implements OutboxEventHandler {

    private final CrmClient crmClient;
    private final JsonMapper jsonMapper;

    @Override
    public boolean supports(OutboxEvent event) {
        return event.getTarget() == OutboxTarget.CRM
                && IntegrationEventTypes.ORDER_PAID.equals(event.getEventType());
    }

    @Override
    public void handle(OutboxEvent event) {
        crmClient.registerSale(jsonMapper.readValue(event.getPayload(), OrderPaidPayload.class));
    }
}