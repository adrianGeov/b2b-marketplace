package com.marketplace.b2b.payment.entity;

public enum PaymentStatus {

    /** La pasarela aprobó el cargo. */
    APPROVED,
    /** La pasarela rechazó el cargo (fondos insuficientes, tarjeta inválida, etc.). */
    DECLINED,
    /** Error técnico de la pasarela (timeout, 5xx) tras agotar reintentos. */
    FAILED

}
