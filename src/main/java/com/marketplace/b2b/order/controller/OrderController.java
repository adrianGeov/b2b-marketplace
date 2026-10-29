package com.marketplace.b2b.order.controller;

import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.order.dto.OrderCreationResult;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.dto.OrderSummaryResponse;
import com.marketplace.b2b.order.service.OrderLifecycleService;
import com.marketplace.b2b.order.service.OrderService;
import com.marketplace.b2b.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
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
@Tag(name = OpenApiConfig.TAG_ORDERS)
public class OrderController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final OrderService orderService;
    private final OrderLifecycleService lifecycleService;

    @PostMapping
    @Operation(summary = "Crear pedido desde el carrito",
            description = "Toma el contenido actual del carrito, bloquea y descuenta existencias, congela precios "
                    + "y vacía el carrito. **Idempotente**: repetir la misma `Idempotency-Key` devuelve el mismo "
                    + "pedido (200 + header `Idempotent-Replayed: true`) en lugar de crear otro.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado (PENDING_PAYMENT)"),
            @ApiResponse(responseCode = "200", description = "Llave ya usada: se devuelve el pedido original"),
            @ApiResponse(responseCode = "400", description = "Falta el header o tiene formato inválido",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Carrito vacío, producto no disponible "
                    + "o existencias insuficientes",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Llave única por intento de compra (8 a 100 caracteres). "
                    + "El front la genera con crypto.randomUUID().", example = "pedido-ana-0001")
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey) {
        OrderCreationResult result = orderService.createOrder(AuthenticatedUser.from(jwt).id(), idempotencyKey);

        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/orders/" + result.order().orderNumber()))
                    .body(result.order());
        }
        return ResponseEntity.ok().header(REPLAYED_HEADER, "true").body(result.order());
    }

    @GetMapping
    @Operation(summary = "Mis pedidos", description = "Paginado, del más reciente al más antiguo.")
    public PageResponse<OrderSummaryResponse> myOrders(
            @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return orderService.getMyOrders(AuthenticatedUser.from(jwt).id(), pageable);
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Detalle y estatus del pedido",
            description = "Un cliente solo ve sus pedidos; el ADMIN ve cualquiera.")
    public OrderResponse getOrder(@AuthenticationPrincipal Jwt jwt,
                                  @Parameter(example = "PED-20261028-8F3K2A") @PathVariable String orderNumber) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return orderService.getOrder(user.id(), orderNumber, user.isAdmin());
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancelar pedido",
            description = "Solo pedidos NO pagados (PENDING_PAYMENT o PAYMENT_FAILED). "
                    + "Las existencias regresan al inventario.")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return lifecycleService.cancel(user.id(), orderNumber, user.isAdmin());
    }
}