package com.marketplace.b2b.cart.service;

import com.marketplace.b2b.cart.dto.AddCartItemRequest;
import com.marketplace.b2b.cart.dto.CartItemResponse;
import com.marketplace.b2b.cart.dto.CartResponse;
import com.marketplace.b2b.cart.dto.UpdateCartItemRequest;
import com.marketplace.b2b.cart.entity.Cart;
import com.marketplace.b2b.cart.entity.CartItem;
import com.marketplace.b2b.cart.mapper.CartMapper;
import com.marketplace.b2b.cart.repository.CartRepository;
import com.marketplace.b2b.catalog.entity.Product;
import com.marketplace.b2b.catalog.repository.ProductRepository;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.user.entity.User;
import com.marketplace.b2b.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @Spy
    private CartMapper cartMapper = Mappers.getMapper(CartMapper.class);

    @InjectMocks
    private CartService cartService;

    // ------------------------------------------------------------------
    // Consultar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Si el usuario no tiene carrito, devuelve uno vacío sin crearlo")
    void shouldReturnEmptyCartWhenNoneExists() {
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        CartResponse cart = cartService.getCart(USER_ID);

        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalItems()).isZero();
        assertThat(cart.total()).isEqualByComparingTo("0");
        verify(cartRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Agregar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Agregar sin carrito previo: crea el carrito y calcula subtotal y total")
    void shouldCreateCartOnFirstItem() {
        Product product = product(10L, "PAP-001", "100.00", 50, true);
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(userRepository.getReferenceById(USER_ID)).willReturn(User.builder().id(USER_ID).build());
        given(cartRepository.save(any(Cart.class))).willAnswer(invocation -> invocation.getArgument(0));

        CartResponse cart = cartService.addItem(USER_ID, new AddCartItemRequest(10L, 3));

        verify(cartRepository).save(any(Cart.class));
        assertThat(cart.items()).hasSize(1);
        CartItemResponse item = cart.items().get(0);
        assertThat(item.sku()).isEqualTo("PAP-001");
        assertThat(item.quantity()).isEqualTo(3);
        assertThat(item.subtotal()).isEqualByComparingTo("300.00");
        assertThat(item.available()).isTrue();
        assertThat(cart.totalItems()).isEqualTo(3);
        assertThat(cart.total()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("Agregar un producto que ya está en el carrito suma la cantidad")
    void shouldIncrementQuantityOfExistingItem() {
        Product product = product(10L, "PAP-001", "100.00", 50, true);
        Cart cart = cartWith(product, 2);
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        CartResponse response = cartService.addItem(USER_ID, new AddCartItemRequest(10L, 3));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.total()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Si la suma supera las existencias lanza INSUFFICIENT_STOCK y no modifica el carrito")
    void shouldRejectQuantityAboveStock() {
        Product product = product(10L, "PAP-001", "100.00", 4, true);
        Cart cart = cartWith(product, 3);
        given(productRepository.findById(10L)).willReturn(Optional.of(product));
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(10L, 2)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("No se puede agregar un producto inactivo")
    void shouldRejectInactiveProduct() {
        given(productRepository.findById(10L)).willReturn(Optional.of(product(10L, "PAP-001", "100.00", 50, false)));

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(10L, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_UNAVAILABLE);
    }

    @Test
    @DisplayName("Agregar un producto inexistente lanza RESOURCE_NOT_FOUND")
    void shouldRejectUnknownProduct() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(99L, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ------------------------------------------------------------------
    // Actualizar / quitar / vaciar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Actualizar reemplaza la cantidad (no la suma)")
    void shouldReplaceQuantity() {
        Product product = product(10L, "PAP-001", "100.00", 50, true);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cartWith(product, 2)));

        CartResponse response = cartService.updateItem(USER_ID, 10L, new UpdateCartItemRequest(7));

        assertThat(response.items().get(0).quantity()).isEqualTo(7);
        assertThat(response.total()).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("Actualizar un producto que no está en el carrito lanza RESOURCE_NOT_FOUND")
    void shouldFailUpdatingMissingItem() {
        Product product = product(10L, "PAP-001", "100.00", 50, true);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cartWith(product, 2)));

        assertThatThrownBy(() -> cartService.updateItem(USER_ID, 77L, new UpdateCartItemRequest(1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("Quitar un producto lo elimina del carrito y recalcula el total")
    void shouldRemoveItem() {
        Product product = product(10L, "PAP-001", "100.00", 50, true);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cartWith(product, 2)));

        CartResponse response = cartService.removeItem(USER_ID, 10L);

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Vaciar elimina todas las líneas del carrito")
    void shouldClearCart() {
        Cart cart = cartWith(product(10L, "PAP-001", "100.00", 50, true), 2);
        given(cartRepository.findByUserId(USER_ID)).willReturn(Optional.of(cart));

        cartService.clear(USER_ID);

        assertThat(cart.getItems()).isEmpty();
    }

    // ------------------------------------------------------------------
    // Datos de prueba
    // ------------------------------------------------------------------

    private static Product product(Long id, String sku, String price, int stock, boolean active) {
        return Product.builder()
                .id(id)
                .sku(sku)
                .name("Producto " + sku)
                .category("Papelería")
                .price(new BigDecimal(price))
                .stock(stock)
                .active(active)
                .build();
    }

    private static Cart cartWith(Product product, int quantity) {
        Cart cart = Cart.builder().id(100L).user(User.builder().id(USER_ID).build()).build();
        cart.addItem(CartItem.builder().product(product).quantity(quantity).build());
        return cart;
    }
}