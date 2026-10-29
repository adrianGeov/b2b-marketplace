package com.marketplace.b2b.cart.controller;

import com.marketplace.b2b.cart.dto.AddCartItemRequest;
import com.marketplace.b2b.cart.dto.CartResponse;
import com.marketplace.b2b.cart.dto.UpdateCartItemRequest;
import com.marketplace.b2b.cart.service.CartService;
import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carrito del usuario autenticado. El id se toma del token, nunca de la URL,
 * así un usuario no puede operar el carrito de otro.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_CART)
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Ver mi carrito", description = "Líneas con precio actual, subtotales y total.")
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(userId(jwt));
    }

    @PostMapping("/items")
    @Operation(summary = "Agregar producto",
            description = "Si el producto ya está en el carrito, SUMA la cantidad. Valida existencias.")
    public CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(userId(jwt), request);
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Cambiar cantidad", description = "Reemplaza la cantidad de la línea.")
    public CartResponse updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(userId(jwt), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Quitar producto")
    public CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        return cartService.removeItem(userId(jwt), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Vaciar carrito")
    public void clear(@AuthenticationPrincipal Jwt jwt) {
        cartService.clear(userId(jwt));
    }

    private static Long userId(Jwt jwt) {
        return AuthenticatedUser.from(jwt).id();
    }
}