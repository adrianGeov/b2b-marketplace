package com.marketplace.b2b.order.service;

import com.marketplace.b2b.catalog.entity.Product;
import com.marketplace.b2b.catalog.repository.ProductRepository;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.entity.OrderItem;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.mapper.OrderMapper;
import com.marketplace.b2b.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    private static final Long USER_ID = 1L;
    private static final String FOLIO = "PED-20261028-AAAAAA";
    private static final Instant NOW = Instant.parse("2026-10-30T18:00:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;

    private OrderLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new OrderLifecycleService(orderRepository, productRepository,
                Mappers.getMapper(OrderMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ------------------------------------------------------------------
    // Cancelación
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Cancelar un pedido pendiente de pago lo deja CANCELLED y devuelve las existencias")
    void shouldCancelPendingOrderAndRestoreStock() {
        Product paper = product(1L, 498);
        Product coffee = product(7L, 397);
        PurchaseOrder order = order(OrderStatus.PENDING_PAYMENT, item(paper, 2), item(coffee, 3));
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID)).willReturn(Optional.of(order));
        given(productRepository.findAllByIdForUpdate(List.of(1L, 7L))).willReturn(List.of(paper, coffee));

        OrderResponse response = service.cancel(USER_ID, FOLIO, false);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(paper.getStock()).isEqualTo(500);
        assertThat(coffee.getStock()).isEqualTo(400);
    }

    @Test
    @DisplayName("Un pedido ya pagado no se puede cancelar y no se tocan las existencias")
    void shouldNotCancelPaidOrder() {
        PurchaseOrder order = order(OrderStatus.PAID, item(product(1L, 498), 2));
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(USER_ID, FOLIO, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ORDER_STATE);

        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("Un cliente no puede cancelar el pedido de otro usuario (404)")
    void shouldNotCancelOrderOfAnotherUser() {
        given(orderRepository.findByOrderNumberAndUserId(FOLIO, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(USER_ID, FOLIO, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ------------------------------------------------------------------
    // Envío y entrega
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un pedido confirmado por el ERP se puede enviar")
    void shouldShipConfirmedOrder() {
        given(orderRepository.findByOrderNumber(FOLIO)).willReturn(Optional.of(order(OrderStatus.CONFIRMED)));

        assertThat(service.ship(FOLIO).status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("Un pedido pagado pero NO confirmado por el ERP no se puede enviar")
    void shouldNotShipOrderNotConfirmedByErp() {
        given(orderRepository.findByOrderNumber(FOLIO)).willReturn(Optional.of(order(OrderStatus.PAID)));

        assertThatThrownBy(() -> service.ship(FOLIO))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ORDER_STATE);
    }

    @Test
    @DisplayName("Entregar registra la fecha real de entrega")
    void shouldDeliverShippedOrderAndRecordDate() {
        PurchaseOrder order = order(OrderStatus.SHIPPED);
        given(orderRepository.findByOrderNumber(FOLIO)).willReturn(Optional.of(order));

        OrderResponse response = service.deliver(FOLIO);

        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(response.deliveredAt()).isEqualTo(NOW);
        assertThat(order.getDeliveredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("No se puede entregar un pedido que no se ha enviado")
    void shouldNotDeliverOrderNotShipped() {
        given(orderRepository.findByOrderNumber(FOLIO)).willReturn(Optional.of(order(OrderStatus.CONFIRMED)));

        assertThatThrownBy(() -> service.deliver(FOLIO))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ORDER_STATE);
    }

    // ------------------------------------------------------------------

    private static Product product(Long id, int stock) {
        return Product.builder()
                .id(id).sku("SKU-" + id).name("Producto " + id).category("General")
                .price(new BigDecimal("100.00")).stock(stock).active(true)
                .build();
    }

    private static OrderItem item(Product product, int quantity) {
        return OrderItem.builder()
                .product(product).sku(product.getSku()).productName(product.getName())
                .unitPrice(product.getPrice()).quantity(quantity)
                .subtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    private static PurchaseOrder order(OrderStatus status, OrderItem... items) {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(50L).orderNumber(FOLIO).status(status).idempotencyKey("pedido-ana-0001")
                .totalAmount(new BigDecimal("100.00")).estimatedDeliveryDate(LocalDate.of(2026, 10, 31))
                .build();
        for (OrderItem item : items) {
            order.addItem(item);
        }
        return order;
    }
}