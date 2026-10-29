package com.marketplace.b2b.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Alta / actualización de producto (solo ADMIN).
 *
 * @param active opcional: en alta, si no se envía queda activo; en actualización, si no se envía no cambia.
 */
public record ProductRequest(

        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 40, message = "El SKU no debe exceder 40 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El SKU solo admite letras, números y guiones")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe exceder 150 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción no debe exceder 1000 caracteres")
        String description,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(max = 80, message = "La categoría no debe exceder 80 caracteres")
        String category,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "El precio admite máximo 10 enteros y 2 decimales")
        BigDecimal price,

        @NotNull(message = "Las existencias son obligatorias")
        @PositiveOrZero(message = "Las existencias no pueden ser negativas")
        Integer stock,

        Boolean active
) {
}