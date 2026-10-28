package com.marketplace.b2b.user.entity;

import com.marketplace.b2b.common.entity.AuditableEntity;
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

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String fullName;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String companyName;

    @Column(name = "rfc", nullable = false, length = 13)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Role role;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean enabled = true;

    /** Identificador asignado por el CRM tras la sincronización. */
    @Column(name = "id_cliente_crm", length = 50)
    private String crmCustomerId;
}