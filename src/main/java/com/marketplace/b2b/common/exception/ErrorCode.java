package com.marketplace.b2b.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catálogo centralizado de errores de la API.
 * Cada código define el estatus HTTP y un título legible; el detalle específico
 * se envía al lanzar la excepción.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ---------- 400 Bad Request ----------
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Petición mal formada"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "Falta un parámetro o header obligatorio"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Parámetro con formato inválido"),

    // ---------- 401 / 403 ----------
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Autenticación requerida"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Acceso denegado"),

    // ---------- 404 / 405 ----------
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso no encontrado"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido"),

    // ---------- 409 Conflict ----------
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Correo ya registrado"),
    SKU_ALREADY_EXISTS(HttpStatus.CONFLICT, "SKU duplicado"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Modificación concurrente"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "Violación de integridad de datos"),

    // ---------- 422 Reglas de negocio ----------
    EMPTY_CART(HttpStatus.UNPROCESSABLE_ENTITY, "Carrito vacío"),
    INSUFFICIENT_STOCK(HttpStatus.UNPROCESSABLE_ENTITY, "Existencias insuficientes"),
    PRODUCT_UNAVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Producto no disponible"),
    INVALID_ORDER_STATE(HttpStatus.UNPROCESSABLE_ENTITY, "Estatus de pedido inválido para la operación"),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.UNPROCESSABLE_ENTITY, "Llave de idempotencia reutilizada"),
    PAYMENT_DECLINED(HttpStatus.UNPROCESSABLE_ENTITY, "Pago rechazado"),

    // ---------- 502 / 503 Servicios externos ----------
    PAYMENT_GATEWAY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Pasarela de pago no disponible"),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "Error en servicio externo"),

    // ---------- 500 ----------
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");

    private final HttpStatus status;
    private final String title;
}