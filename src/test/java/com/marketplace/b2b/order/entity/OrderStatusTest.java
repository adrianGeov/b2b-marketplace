package com.marketplace.b2b.order.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} está permitido")
    @CsvSource({
            "PENDING_PAYMENT, PAID",
            "PENDING_PAYMENT, PAYMENT_FAILED",
            "PENDING_PAYMENT, CANCELLED",
            "PAYMENT_FAILED,  PAID",
            "PAID,            CONFIRMED",
            "CONFIRMED,       SHIPPED",
            "SHIPPED,         DELIVERED"
    })
    void shouldAllowValidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} NO está permitido")
    @CsvSource({
            "PAID,            CANCELLED",
            "PAID,            SHIPPED",
            "PENDING_PAYMENT, SHIPPED",
            "CONFIRMED,       DELIVERED",
            "DELIVERED,       CANCELLED",
            "CANCELLED,       PAID"
    })
    void shouldRejectInvalidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }
}