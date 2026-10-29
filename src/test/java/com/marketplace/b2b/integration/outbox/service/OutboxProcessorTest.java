package com.marketplace.b2b.integration.outbox.service;

import com.marketplace.b2b.integration.IntegrationException;
import com.marketplace.b2b.integration.config.IntegrationProperties;
import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.integration.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    private static final Instant NOW = Instant.parse("2026-10-28T18:00:00Z");

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private OutboxEventHandler handler;

    private OutboxProcessor processor;

    @BeforeEach
    void setUp() {
        // 3 intentos máximos, backoff inicial de 10 s
        IntegrationProperties properties = new IntegrationProperties(
                new IntegrationProperties.Outbox(20, 3, Duration.ofSeconds(10), Duration.ofMinutes(10)), null, null);
        processor = new OutboxProcessor(repository, List.of(handler), properties, null,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Si no hay eventos pendientes, no hace nada")
    void shouldReturnFalseWhenNothingPending() {
        given(repository.lockNextBatch(1)).willReturn(List.of());

        assertThat(processor.processNext()).isFalse();
        verifyNoInteractions(handler);
    }

    @Test
    @DisplayName("Evento enviado con éxito queda PROCESSED")
    void shouldMarkEventAsProcessed() {
        OutboxEvent event = event(0);
        given(repository.lockNextBatch(1)).willReturn(List.of(event));
        given(handler.supports(event)).willReturn(true);

        assertThat(processor.processNext()).isTrue();

        verify(handler).handle(event);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("ERP caído: el evento sigue PENDING y se reprograma con backoff (10 s)")
    void shouldRescheduleOnFailure() {
        OutboxEvent event = event(0);
        given(repository.lockNextBatch(1)).willReturn(List.of(event));
        given(handler.supports(event)).willReturn(true);
        willThrow(new IntegrationException("ERP no disponible")).given(handler).handle(event);

        processor.processNext();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(event.getLastError()).isEqualTo("ERP no disponible");
    }

    @Test
    @DisplayName("El backoff crece exponencialmente: segundo fallo espera 20 s")
    void shouldIncreaseBackoffExponentially() {
        OutboxEvent event = event(1);
        given(repository.lockNextBatch(1)).willReturn(List.of(event));
        given(handler.supports(event)).willReturn(true);
        willThrow(new IntegrationException("ERP no disponible")).given(handler).handle(event);

        processor.processNext();

        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(20));
    }

    @Test
    @DisplayName("Al llegar al máximo de intentos el evento queda FAILED")
    void shouldMarkEventAsFailedAfterMaxAttempts() {
        OutboxEvent event = event(2);
        given(repository.lockNextBatch(1)).willReturn(List.of(event));
        given(handler.supports(event)).willReturn(true);
        willThrow(new IntegrationException("ERP no disponible")).given(handler).handle(event);

        processor.processNext();

        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    private static OutboxEvent event(int previousAttempts) {
        return OutboxEvent.builder()
                .id(1L)
                .aggregateType("ORDER")
                .aggregateId("PED-20261028-AAAAAA")
                .target(OutboxTarget.ERP)
                .eventType("ORDER_PAID")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .attempts(previousAttempts)
                .nextAttemptAt(NOW)
                .build();
    }
}