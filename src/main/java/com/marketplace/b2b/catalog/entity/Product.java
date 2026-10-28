package com.marketplace.b2b.catalog.entity;

import com.marketplace.b2b.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false, unique = true, length = 40)
    private String sku;

    @Column(name = "nombre", nullable = false, length = 150)
    private String name;

    @Column(name = "descripcion", length = 1000)
    private String description;

    @Column(name = "categoria", nullable = false, length = 80)
    private String category;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "existencias", nullable = false)
    private Integer stock;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean active = true;

    /** Bloqueo optimista: evita que dos transacciones sobrescriban las existencias al mismo tiempo. */
    @Version
    @Column(name = "version")
    private Long version;
}