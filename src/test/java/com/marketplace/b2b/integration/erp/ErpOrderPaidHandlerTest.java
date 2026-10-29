package com.marketplace.b2b.integration.erp;

import com.marketplace.b2b.integration.event.OrderPaidPayload;
import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
import com.marketplace.b2b.integration.outbox.entity.OutboxTarget;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ErpOrderPaidHandlerTest {

    private static final String FOLIO = "PED-20261028-AAAAAA";

    @Mock
    private ErpClient erpClient;
    @Mock
    private OrderRepository orderRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private ErpOrderPaidHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ErpOrderPaidHandler(erpClient, orderRepository, jsonMapper);
    }

    @Test
    @DisplayName("Solo atiende eventos ORDER_PAID con destino ERP")
    void shouldSupportOnlyErpOrderPaid() {
        assertThat(handler.supports(event(OutboxTarget.ERP, "{}"))).isTrue();
        assertThat(handler.supports(event(OutboxTarget.CRM, "{}"))).isFalse();
    }

    @Test
    @DisplayName("Crea la orden de venta en el ERP y deja el pedido CONFIRMED con su id del ERP")
    void shouldConfirmOrderWithErpId() {
        OrderPaidPayload payload = new OrderPaidPayload(FOLIO, 10L, "compras@acme.mx", "Acme SA de CV",
                "AAA010101AAA", new BigDecimal("1798.00"), "PAY-123", LocalDate.of(2026, 10, 31),
                List.of(new OrderPaidPayload.Line("PAP-001", "Hojas", 2,
                        new BigDecimal("899.00"), new BigDecimal("1798.00"))));
        PurchaseOrder order = PurchaseOrder.builder()
                .id(50L).orderNumber(FOLIO).status(OrderStatus.PAID)
                .idempotencyKey("pedido-ana-0001").totalAmount(new BigDecimal("1798.00"))
                .build();

        given(erpClient.createSalesOrder(any(OrderPaidPayload.class))).willReturn("ERP-0001");
        given(orderRepository.findByOrderNumber(FOLIO)).willReturn(Optional.of(order));

        handler.handle(event(OutboxTarget.ERP, jsonMapper.writeValueAsString(payload)));

        ArgumentCaptor<OrderPaidPayload> sent = ArgumentCaptor.forClass(OrderPaidPayload.class);
        verify(erpClient).createSalesOrder(sent.capture());
        assertThat(sent.getValue()).isEqualTo(payload);

        assertThat(order.getErpOrderId()).isEqualTo("ERP-0001");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    private static OutboxEvent event(OutboxTarget target, String payload) {
        return OutboxEvent.builder()
                .id(1L).aggregateType("ORDER").aggregateId(FOLIO)
                .target(target).eventType("ORDER_PAID").payload(payload)
                .status(OutboxStatus.PENDING).attempts(0)
                .build();
    }
}