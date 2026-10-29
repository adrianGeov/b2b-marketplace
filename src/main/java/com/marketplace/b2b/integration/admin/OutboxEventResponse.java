package com.marketplace.b2b.integration.admin;

import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;

import java.time.Instant;

/**
 * Ejemplo de mapeo MANUAL (el proyecto combina MapStruct y mapeo manual donde es más simple).
 */
public record OutboxEventResponse(
        Long id,
        OutboxTarget target,
        String eventType,
        String aggregateId,
        OutboxStatus status,
        Integer attempts,
        String lastError,
        Instant nextAttemptAt,
        Instant createdAt,
        Instant processedAt
) {

    public static OutboxEventResponse from(OutboxEvent event) {
        return new OutboxEventResponse(event.getId(), event.getTarget(), event.getEventType(), event.getAggregateId(),
                event.getStatus(), event.getAttempts(), event.getLastError(), event.getNextAttemptAt(),
                event.getCreatedAt(), event.getProcessedAt());
    }
}