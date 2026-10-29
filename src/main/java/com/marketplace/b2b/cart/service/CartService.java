package com.marketplace.b2b.cart.service;

import com.marketplace.b2b.cart.dto.AddCartItemRequest;
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
import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import com.marketplace.b2b.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QUANTITY_PER_PRODUCT = 999;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cartMapper::toResponse)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", request.productId()));
        ensureAvailable(product);

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));
        Optional<CartItem> existing = findItem(cart, product.getId());

        // Si el producto ya estaba en el carrito, se suma la cantidad
        int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + request.quantity();
        validateQuantity(product, newQuantity);

        existing.ifPresentOrElse(
                item -> item.setQuantity(newQuantity),
                () -> cart.addItem(CartItem.builder().product(product).quantity(newQuantity).build()));

        log.debug("Carrito usuarioId={} producto={} cantidad={}", userId, product.getSku(), newQuantity);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> itemNotFound(productId));
        CartItem item = findItem(cart, productId).orElseThrow(() -> itemNotFound(productId));

        ensureAvailable(item.getProduct());
        validateQuantity(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> itemNotFound(productId));
        CartItem item = findItem(cart, productId).orElseThrow(() -> itemNotFound(productId));

        cart.removeItem(item);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public void clear(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(Cart::clear);
    }

    // ------------------------------------------------------------------

    private Cart createCart(Long userId) {
        log.debug("Creando carrito para usuarioId={}", userId);
        Cart cart = Cart.builder().user(userRepository.getReferenceById(userId)).build();
        return cartRepository.save(cart);
    }

    private static Optional<CartItem> findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    private static void ensureAvailable(Product product) {
        if (!product.isActive()) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE,
                    "El producto " + product.getSku() + " no está disponible");
        }
    }

    private static void validateQuantity(Product product, int quantity) {
        if (quantity > MAX_QUANTITY_PER_PRODUCT) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                    "La cantidad máxima por producto es " + MAX_QUANTITY_PER_PRODUCT);
        }
        if (quantity > product.getStock()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Existencias insuficientes para el SKU " + product.getSku()
                            + ": disponibles " + product.getStock() + ", solicitadas " + quantity);
        }
    }

    private static ResourceNotFoundException itemNotFound(Long productId) {
        return new ResourceNotFoundException("Producto en el carrito", productId);
    }
}