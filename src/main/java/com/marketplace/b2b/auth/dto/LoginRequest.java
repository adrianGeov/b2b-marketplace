package com.marketplace.b2b.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Schema(example = "admin@marketplace.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email,

        @Schema(example = "Admin123!")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}