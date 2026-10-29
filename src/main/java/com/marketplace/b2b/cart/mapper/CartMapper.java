package com.marketplace.b2b.cart.mapper;

import com.marketplace.b2b.cart.dto.CartItemResponse;
import com.marketplace.b2b.cart.dto.CartResponse;
import com.marketplace.b2b.cart.entity.Cart;
import com.marketplace.b2b.cart.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "sku", source = "product.sku")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "unitPrice", source = "product.price")
    @Mapping(target = "availableStock", source = "product.stock")
    @Mapping(target = "subtotal",
            expression = "java(item.getProduct().getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    @Mapping(target = "available",
            expression = "java(item.getProduct().isActive() && item.getProduct().getStock() >= item.getQuantity())")
    CartItemResponse toItemResponse(CartItem item);

    /** MapStruct no genera los métodos default: este arma el carrito con sus totales. */
    default CartResponse toResponse(Cart cart) {
        return CartResponse.of(cart.getItems().stream().map(this::toItemResponse).toList());
    }
}