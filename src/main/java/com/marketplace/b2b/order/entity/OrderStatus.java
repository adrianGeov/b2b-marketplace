package com.marketplace.b2b.order.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estatus del pedido y sus transiciones permitidas (máquina de estados).
 */
public enum OrderStatus {
    /** Pedido creado, esperando pago. */
    PENDING_PAYMENT,
    /** La pasarela rechazó o falló el pago; se puede reintentar. */
    PAYMENT_FAILED,
    /** Pago aprobado; pendiente de confirmación por el ERP. */
    PAID,
    /** El ERP aceptó el pedido (tiene id_pedido_erp). */
    CONFIRMED,
    /** Enviado al cliente. */
    SHIPPED,
    /** Entregado (se registra la fecha real de entrega). */
    DELIVERED,
    /** Cancelado antes del pago; las existencias se devolvieron al inventario. */
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    private Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(PAYMENT_FAILED, PAID, CANCELLED);
            case PAYMENT_FAILED -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(CONFIRMED);
            case CONFIRMED -> EnumSet.of(SHIPPED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}