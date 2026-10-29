package com.marketplace.b2b.integration.outbox.service;

import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.integration.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final JsonMapper jsonMapper;

    /**
     * MANDATORY: falla si no hay una transacción activa. Garantiza que el evento se guarde
     * junto con el cambio de negocio que lo origina (o ninguno de los dos).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String aggregateType, String aggregateId, OutboxTarget target, String eventType,
                        Object payload) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .target(target)
                .eventType(eventType)
                .payload(jsonMapper.writeValueAsString(payload))
                .build();
        repository.save(event);
        log.debug("Evento encolado {} -> {} ({} {})", eventType, target, aggregateType, aggregateId);
    }
}