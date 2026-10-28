package com.marketplace.b2b.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no debe exceder 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "La contraseña debe tener entre 8 y 72 caracteres e incluir al menos una letra y un número")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre no debe exceder 150 caracteres")
        String fullName,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no debe exceder 150 caracteres")
        String companyName,

        @NotBlank(message = "El RFC es obligatorio")
        @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El RFC no tiene un formato válido")
        String taxId,

        @NotNull(message = "El tipo de cuenta es obligatorio (DISTRIBUTOR o CUSTOMER)")
        AccountType accountType
) {
}