package com.marketplace.b2b.order.service;

import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.order.dto.OrderCreationResult;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.mapper.OrderMapper;
import com.marketplace.b2b.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 1L;
    private static final String KEY = "pedido-ana-0001";

    @Mock
    private OrderPlacementService placementService;
    @Mock
    private OrderRepository orderRepository;

    @Spy
    private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Primera petición con una llave nueva: crea el pedido (created = true)")
    void shouldCreateOrderWithNewKey() {
        given(orderRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY)).willReturn(Optional.empty());
        given(placementService.place(USER_ID, KEY)).willReturn(order("PED-20261028-AAAAAA"));

        OrderCreationResult result = orderService.createOrder(USER_ID, KEY);

        assertThat(result.created()).isTrue();
        assertThat(result.order().orderNumber()).isEqualTo("PED-20261028-AAAAAA");
        assertThat(result.order().total()).isEqualByComparingTo("2665.00");
    }

    @Test
    @DisplayName("Repetir la misma llave devuelve el pedido original y NO crea otro")
    void shouldReplayExistingOrder() {
        given(orderRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY))
                .willReturn(Optional.of(order("PED-20261028-AAAAAA")));

        OrderCreationResult result = orderService.createOrder(USER_ID, KEY);

        assertThat(result.created()).isFalse();
        assertThat(result.order().orderNumber()).isEqualTo("PED-20261028-AAAAAA");
        verify(placementService, never()).place(anyLong(), anyString());
    }

    @Test
    @DisplayName("Carrera: si la BD rechaza el duplicado, devuelve el pedido que ganó")
    void shouldRecoverFromConcurrentDuplicate() {
        given(orderRepository.findByUserIdAndIdempotencyKey(USER_ID, KEY))
                .willReturn(Optional.empty(), Optional.of(order("PED-20261028-GANADOR")));
        given(placementService.place(USER_ID, KEY))
                .willThrow(new DataIntegrityViolationException("uk_pedidos_usuario_llave"));

        OrderCreationResult result = orderService.createOrder(USER_ID, KEY);

        assertThat(result.created()).isFalse();
        assertThat(result.order().orderNumber()).isEqualTo("PED-20261028-GANADOR");
    }

    @Test
    @DisplayName("Una Idempotency-Key con formato inválido se rechaza antes de tocar la BD")
    void shouldRejectInvalidKey() {
        assertThatThrownBy(() -> orderService.createOrder(USER_ID, "abc"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        verify(placementService, never()).place(anyLong(), anyString());
    }

    @Test
    @DisplayName("Un cliente no puede ver el pedido de otro usuario (404)")
    void shouldNotExposeOrdersOfOtherUsers() {
        given(orderRepository.findByOrderNumberAndUserId("PED-AJENO", USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(USER_ID, "PED-AJENO", false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private static PurchaseOrder order(String orderNumber) {
        return PurchaseOrder.builder()
                .id(50L)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING_PAYMENT)
                .idempotencyKey(KEY)
                .totalAmount(new BigDecimal("2665.00"))
                .build();
    }
}