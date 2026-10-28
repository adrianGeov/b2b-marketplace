package com.marketplace.b2b.order.entity;

public enum OrderStatus {

     /** Pedido creado, esperando pago. */
    PENDING_PAYMENT,
    /** La pasarela rechazó o falló el pago; se puede reintentar. */
    PAYMENT_FAILED,
    /** Pago aprobado; pendiente de confirmación por el ERP. */
    PAID,
    /** El ERP aceptó el pedido (tiene id_pedido_erp). */
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED

}
