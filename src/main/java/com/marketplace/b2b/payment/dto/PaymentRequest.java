package com.marketplace.b2b.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Por cumplimiento PCI DSS la API nunca recibe el número de tarjeta:
 * el front lo tokeniza con el SDK de la pasarela y solo envía el token.
 */
public record PaymentRequest(

        @Schema(example = "tok_visa", description = "tok_visa | tok_declined | tok_gateway_down")
        @NotBlank(message = "El token de pago es obligatorio")
        @Size(max = 100, message = "El token de pago no debe exceder 100 caracteres")
        @Pattern(regexp = "^tok_[A-Za-z0-9_]+$", message = "El token de pago no tiene un formato válido (tok_...)")
        String cardToken
) {
}