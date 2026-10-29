package com.marketplace.b2b.order.dto;

/**
 * @param created true si se creó un pedido nuevo; false si se devolvió uno existente
 *                por repetir la misma Idempotency-Key.
 */
public record OrderCreationResult(OrderResponse order, boolean created) {
}