package com.marketplace.b2b.common.exception;

/**
 * Error de validación de un campo específico.
 * No se incluye el valor rechazado para no exponer datos sensibles 
 */
public record FieldErrorDetail(String field, String message) {
}