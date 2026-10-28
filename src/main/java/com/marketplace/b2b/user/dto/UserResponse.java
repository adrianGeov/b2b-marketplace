package com.marketplace.b2b.user.dto;

import com.marketplace.b2b.user.entity.Role;

/**
 * Datos públicos del usuario. Nunca incluye el hash de la contraseña.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String companyName,
        String taxId,
        Role role
) {
}