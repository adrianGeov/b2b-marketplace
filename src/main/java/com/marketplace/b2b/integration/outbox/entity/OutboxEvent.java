package com.marketplace.b2b.integration.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Patrón Transactional Outbox: el evento se guarda en la MISMA transacción que el pedido,
 * y un proceso en segundo plano lo envía al ERP/CRM con reintentos.
 * Si el ERP está caído, la venta no se pierde ni se bloquea al usuario.
 */
@Entity
@Table(name = "eventos_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_agregado", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "id_agregado", nullable = false, length = 50)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "destino", nullable = false, length = 10)
    private OutboxTarget target;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String eventType;

    @Column(name = "contenido", nullable = false, columnDefinition = "text")
    private String payload;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Builder.Default
    @Column(name = "intentos", nullable = false)
    private Integer attempts = 0;

    @Column(name = "ultimo_error", length = 500)
    private String lastError;

    @Builder.Default
    @Column(name = "fecha_siguiente_intento", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "fecha_procesado")
    private Instant processedAt;
}