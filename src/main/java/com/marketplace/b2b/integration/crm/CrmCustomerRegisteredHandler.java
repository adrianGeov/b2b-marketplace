package com.marketplace.b2b.integration.crm;

import com.marketplace.b2b.integration.event.CustomerRegisteredPayload;
import com.marketplace.b2b.integration.event.IntegrationEventTypes;
import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.integration.outbox.service.OutboxEventHandler;
import com.marketplace.b2b.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente registrado → alta en el CRM → se guarda id_cliente_crm en el usuario.
 */
@Component
@RequiredArgsConstructor
public class CrmCustomerRegisteredHandler implements OutboxEventHandler {

    private final CrmClient crmClient;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;

    @Override
    public boolean supports(OutboxEvent event) {
        return event.getTarget() == OutboxTarget.CRM
                && IntegrationEventTypes.CUSTOMER_REGISTERED.equals(event.getEventType());
    }

    @Override
    public void handle(OutboxEvent event) {
        CustomerRegisteredPayload payload = jsonMapper.readValue(event.getPayload(), CustomerRegisteredPayload.class);
        String crmId = crmClient.upsertCustomer(payload);
        userRepository.findById(payload.userId()).ifPresent(user -> user.setCrmCustomerId(crmId));
    }
}