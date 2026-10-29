package com.marketplace.b2b.order.controller;

import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.order.dto.OrderCreationResult;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.dto.OrderSummaryResponse;
import com.marketplace.b2b.order.service.OrderService;
import com.marketplace.b2b.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final OrderService orderService;

    /**
     * Crea un pedido con el contenido actual del carrito.
     * 201 si es nuevo; 200 + Idempotent-Replayed: true si la llave ya se había usado.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey) {
        OrderCreationResult result = orderService.createOrder(AuthenticatedUser.from(jwt).id(), idempotencyKey);

        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/orders/" + result.order().orderNumber()))
                    .body(result.order());
        }
        return ResponseEntity.ok().header(REPLAYED_HEADER, "true").body(result.order());
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> myOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.getMyOrders(AuthenticatedUser.from(jwt).id(), pageable);
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse getOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return orderService.getOrder(user.id(), orderNumber, user.isAdmin());
    }
}