package com.marketplace.b2b.integration.outbox.service;

import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;

/**
 * Cada tipo de evento + destino tiene su handler. Agregar una integración nueva
 * es crear otra implementación, sin tocar el procesador.
 */
public interface OutboxEventHandler {

    boolean supports(OutboxEvent event);

    /**
     * @throws RuntimeException si el sistema externo falla; el procesador reintentará
     */
    void handle(OutboxEvent event);
}