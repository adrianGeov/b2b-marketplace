package com.marketplace.b2b.auth.dto;

import com.marketplace.b2b.user.entity.Role;

/**
 * Tipos de cuenta permitidos en el autoregistro.
 * ADMIN no está aquí a propósito: nadie puede registrarse como administrador desde la API.
 */
public enum AccountType {
    DISTRIBUTOR,
    CUSTOMER;

    public Role toRole() {
        return Role.valueOf(name());
    }
}