package com.marketplace.b2b.order.service;

import com.marketplace.b2b.cart.entity.Cart;
import com.marketplace.b2b.cart.entity.CartItem;
import com.marketplace.b2b.cart.repository.CartRepository;
import com.marketplace.b2b.catalog.entity.Product;
import com.marketplace.b2b.catalog.repository.ProductRepository;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.order.entity.OrderItem;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.repository.OrderRepository;
import com.marketplace.b2b.user.entity.User;
import com.marketplace.b2b.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceTest {

    private static final Long USER_ID = 1L;
    private static final String KEY = "pedido-ana-0001";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-28T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    private OrderPlacementService placementService;

    @BeforeEach
    void setUp() {
        placementService = new OrderPlacementService(cartRepository, productRepository, orderRepository,
                userRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Crea el pedido: descuenta existencias, congela precios, calcula total y vacía el carrito")
    void shouldPlaceOrderFromCart() {
        Product paper = product(1L, "PAP-001", "899.00", 500, true);
        Product coffee = product(7L, "CAF-001", "289.00", 400, true);
        Cart cart = cart(item(paper, 2), item(coffee, 3));

        given(cartRepository.findProductIdsByUserId(USER_ID)).willReturn(List.of(1L, 7L));
        given(productRepository.findAllByIdForUpdate(List.of(1L, 7L))).willReturn(List.of(paper, coffee));
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));
        given(userRepository.getReferenceById(USER_ID)).willReturn(User.builder().id(USER_ID).build());
        given(orderRepository.saveAndFlush(any(PurchaseOrder.class))).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder order = placementService.place(USER_ID, KEY);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getIdempotencyKey()).isEqualTo(KEY);
        assertThat(order.getOrderNumber()).matches("PED-20261028-[A-Z0-9]{6}");
        assertThat(order.getEstimatedDeliveryDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(order.getTotalAmount()).isEqualByComparingTo("2665.00"); // 899*2 + 289*3

        assertThat(order.getItems()).extracting(OrderItem::getSku).containsExactly("PAP-001", "CAF-001");
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("899.00");
        assertThat(order.getItems().get(0).getSubtotal()).isEqualByComparingTo("1798.00");

        assertThat(paper.getStock()).isEqualTo(498);
        assertThat(coffee.getStock()).isEqualTo(397);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Carrito vacío lanza EMPTY_CART sin bloquear productos")
    void shouldRejectEmptyCart() {
        given(cartRepository.findProductIdsByUserId(USER_ID)).willReturn(List.of());

        assertThatThrownBy(() -> placementService.place(USER_ID, KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPTY_CART);

        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    @DisplayName("Si UN producto no alcanza, no se descuenta NINGUNA existencia y no se guarda nada")
    void shouldRejectWhenAnyProductHasInsufficientStock() {
        Product paper = product(1L, "PAP-001", "899.00", 500, true);
        Product chair = product(12L, "MOB-001", "2899.00", 1, true);
        Cart cart = cart(item(paper, 2), item(chair, 5));

        given(cartRepository.findProductIdsByUserId(USER_ID)).willReturn(List.of(1L, 12L));
        given(productRepository.findAllByIdForUpdate(List.of(1L, 12L))).willReturn(List.of(paper, chair));
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> placementService.place(USER_ID, KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        assertThat(paper.getStock()).isEqualTo(500);
        assertThat(chair.getStock()).isEqualTo(1);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Un producto desactivado después de agregarlo al carrito lanza PRODUCT_UNAVAILABLE")
    void shouldRejectInactiveProduct() {
        Product paper = product(1L, "PAP-001", "899.00", 500, false);
        Cart cart = cart(item(paper, 1));

        given(cartRepository.findProductIdsByUserId(USER_ID)).willReturn(List.of(1L));
        given(productRepository.findAllByIdForUpdate(List.of(1L))).willReturn(List.of(paper));
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> placementService.place(USER_ID, KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_UNAVAILABLE);
    }

    // ------------------------------------------------------------------

    private static Product product(Long id, String sku, String price, int stock, boolean active) {
        return Product.builder()
                .id(id).sku(sku).name("Producto " + sku).category("General")
                .price(new BigDecimal(price)).stock(stock).active(active)
                .build();
    }

    private static CartItem item(Product product, int quantity) {
        return CartItem.builder().product(product).quantity(quantity).build();
    }

    private static Cart cart(CartItem... items) {
        Cart cart = Cart.builder().id(100L).user(User.builder().id(USER_ID).build()).build();
        for (CartItem item : items) {
            cart.addItem(item);
        }
        return cart;
    }
}