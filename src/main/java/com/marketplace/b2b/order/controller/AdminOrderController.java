package com.marketplace.b2b.order.controller;

import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.dto.OrderSummaryResponse;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.service.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
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
public class AdminOrderController {

    private final OrderLifecycleService lifecycleService;

    /** Ejemplo: /api/v1/admin/orders?status=CONFIRMED&page=0&size=20 */
    @GetMapping
    public PageResponse<OrderSummaryResponse> findAll(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return lifecycleService.findAll(status, pageable);
    }

    @PostMapping("/{orderNumber}/ship")
    public OrderResponse ship(@PathVariable String orderNumber) {
        return lifecycleService.ship(orderNumber);
    }

    @PostMapping("/{orderNumber}/deliver")
    public OrderResponse deliver(@PathVariable String orderNumber) {
        return lifecycleService.deliver(orderNumber);
    }
}