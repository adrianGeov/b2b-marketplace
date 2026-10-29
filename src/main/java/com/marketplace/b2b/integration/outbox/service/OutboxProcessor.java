package com.marketplace.b2b.integration.outbox.service;

import com.marketplace.b2b.common.logging.CorrelationIdFilter;
import com.marketplace.b2b.integration.config.IntegrationProperties;
import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
import com.marketplace.b2b.integration.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Procesa en segundo plano los eventos pendientes del outbox.
 * Cada evento se atiende en SU PROPIA transacción corta, para que un ERP lento
 * no retenga muchos eventos ni conexiones de base de datos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final int MAX_ERROR_LENGTH = 500;

    private final OutboxEventRepository repository;
    private final List<OutboxEventHandler> handlers;
    private final IntegrationProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.integration.outbox.poll-interval:PT5S}",
            initialDelayString = "${app.integration.outbox.poll-interval:PT5S}")
    public void processPending() {
        int batchSize = properties.outbox().batchSize();
        int handled = 0;
        try {
            while (handled < batchSize && Boolean.TRUE.equals(transactionTemplate.execute(status -> processNext()))) {
                handled++;
            }
        } catch (RuntimeException ex) {
            // Error de BD al confirmar: el evento sigue PENDING y se reintenta en el siguiente ciclo
            log.error("Error procesando el outbox; se reintentará en el siguiente ciclo", ex);
        }
        if (handled > 0) {
            log.debug("Outbox: {} evento(s) atendidos en este ciclo", handled);
        }
    }

    /**
     * Atiende UN evento pendiente.
     *
     * @return false si no había eventos pendientes
     */
    boolean processNext() {
        List<OutboxEvent> next = repository.lockNextBatch(1);
        if (next.isEmpty()) {
            return false;
        }

        OutboxEvent event = next.get(0);
        MDC.put(CorrelationIdFilter.MDC_KEY, "outbox-" + event.getId());
        try {
            handlerFor(event).handle(event);
            markProcessed(event);
        } catch (RuntimeException ex) {
            registerFailure(event, ex);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
        return true;
    }

    // ------------------------------------------------------------------

    private OutboxEventHandler handlerFor(OutboxEvent event) {
        return handlers.stream()
                .filter(handler -> handler.supports(event))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay handler para " + event.getEventType() + " -> " + event.getTarget()));
    }

    private void markProcessed(OutboxEvent event) {
        event.setAttempts(event.getAttempts() + 1);
        event.setStatus(OutboxStatus.PROCESSED);
        event.setProcessedAt(Instant.now(clock));
        event.setLastError(null);
        log.info("Evento {} {} -> {} procesado (intento {})",
                event.getId(), event.getEventType(), event.getTarget(), event.getAttempts());
    }

    private void registerFailure(OutboxEvent event, RuntimeException ex) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(truncate(ex.getMessage()));

        if (attempts >= properties.outbox().maxAttempts()) {
            event.setStatus(OutboxStatus.FAILED);
            log.error("Evento {} {} -> {} marcado FAILED tras {} intentos: {}",
                    event.getId(), event.getEventType(), event.getTarget(), attempts, ex.getMessage());
            return;
        }

        Duration delay = backoff(attempts);
        event.setNextAttemptAt(Instant.now(clock).plus(delay));
        log.warn("Evento {} {} -> {} falló (intento {}/{}); siguiente intento en {} s: {}",
                event.getId(), event.getEventType(), event.getTarget(), attempts,
                properties.outbox().maxAttempts(), delay.toSeconds(), ex.getMessage());
    }

    /** Backoff exponencial: 10 s, 20 s, 40 s... con un tope máximo. */
    private Duration backoff(int attempts) {
        Duration initial = properties.outbox().initialBackoff();
        Duration max = properties.outbox().maxBackoff();
        Duration delay = initial.multipliedBy(1L << Math.min(attempts - 1, 20));
        return delay.compareTo(max) > 0 ? max : delay;
    }

    private static String truncate(String message) {
        if (message == null) {
            return "Error sin mensaje";
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}