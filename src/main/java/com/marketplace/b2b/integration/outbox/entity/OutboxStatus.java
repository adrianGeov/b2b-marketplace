package com.marketplace.b2b.integration.outbox.entity;

public enum OutboxStatus {

    /** Pendiente de enviar (o esperando el siguiente reintento). */
    PENDING,
    /** Enviado y aceptado por el sistema externo. */
    PROCESSED,
    /** Se agotaron los reintentos; requiere revisión manual. */
    FAILED

}
