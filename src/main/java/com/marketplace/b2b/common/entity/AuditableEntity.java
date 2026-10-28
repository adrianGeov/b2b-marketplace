package com.marketplace.b2b.common.entity;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Campos de auditoría comunes.
 */
@Getter
@MappedSuperclass
public abstract class AuditableEntity {

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant updatedAt;
}