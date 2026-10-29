package com.marketplace.b2b.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propiedades app.integration.*
 */
@ConfigurationProperties(prefix = "app.integration")
public record IntegrationProperties(Outbox outbox, ExternalSystem erp, ExternalSystem crm) {

    public IntegrationProperties {
        if (outbox == null) {
            outbox = new Outbox(0, 0, null, null);
        }
        if (erp == null) {
            erp = new ExternalSystem(null, null);
        }
        if (crm == null) {
            crm = new ExternalSystem(null, null);
        }
    }

    /**
     * @param batchSize      eventos máximos por ciclo del procesador
     * @param maxAttempts    intentos antes de marcar el evento como FAILED
     * @param initialBackoff espera antes del primer reintento (se duplica en cada fallo)
     * @param maxBackoff     espera máxima entre reintentos
     */
    public record Outbox(int batchSize, int maxAttempts, Duration initialBackoff, Duration maxBackoff) {

        public Outbox {
            if (batchSize <= 0) {
                batchSize = 20;
            }
            if (maxAttempts <= 0) {
                maxAttempts = 8;
            }
            if (initialBackoff == null) {
                initialBackoff = Duration.ofSeconds(10);
            }
            if (maxBackoff == null) {
                maxBackoff = Duration.ofMinutes(10);
            }
        }
    }

    /**
     * Configuración de la simulación de un sistema externo.
     */
    public record ExternalSystem(Boolean available, Duration latency) {

        public ExternalSystem {
            if (available == null) {
                available = Boolean.TRUE;
            }
            if (latency == null) {
                latency = Duration.ZERO;
            }
        }
    }
}