package com.marketplace.b2b.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Por cumplimiento PCI DSS la API nunca recibe el número de tarjeta:
 * el front lo tokeniza con el SDK de la pasarela y solo envía el token.
 */
public record PaymentRequest(

        @NotBlank(message = "El token de pago es obligatorio")
        @Size(max = 100, message = "El token de pago no debe exceder 100 caracteres")
        @Pattern(regexp = "^tok_[A-Za-z0-9_]+$", message = "El token de pago no tiene un formato válido (tok_...)")
        String cardToken
) {
}