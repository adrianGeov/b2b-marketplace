package com.marketplace.b2b.order.controller;

import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.dto.OrderSummaryResponse;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.service.OrderLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operación de pedidos para el administrador (almacén / logística).
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_ADMIN_ORDERS)
public class AdminOrderController {

    private final OrderLifecycleService lifecycleService;

    @GetMapping
    @Operation(summary = "Listar pedidos de todos los clientes", description = "Filtro opcional por estatus.")
    public PageResponse<OrderSummaryResponse> findAll(
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return lifecycleService.findAll(status, pageable);
    }

    @PostMapping("/{orderNumber}/ship")
    @Operation(summary = "Marcar como enviado", description = "Solo pedidos CONFIRMED por el ERP.")
    public OrderResponse ship(@PathVariable String orderNumber) {
        return lifecycleService.ship(orderNumber);
    }

    @PostMapping("/{orderNumber}/deliver")
    @Operation(summary = "Marcar como entregado",
            description = "Solo pedidos SHIPPED. Registra la fecha real de entrega (KPI a tiempo vs. tardía).")
    public OrderResponse deliver(@PathVariable String orderNumber) {
        return lifecycleService.deliver(orderNumber);
    }
}