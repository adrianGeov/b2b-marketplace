package com.marketplace.b2b.order.service;

import com.marketplace.b2b.catalog.entity.Product;
import com.marketplace.b2b.catalog.repository.ProductRepository;
import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.dto.OrderSummaryResponse;
import com.marketplace.b2b.order.entity.OrderItem;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.mapper.OrderMapper;
import com.marketplace.b2b.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cambios de estatus del pedido posteriores a su creación: cancelar, enviar y entregar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "totalAmount", "status", "estimatedDeliveryDate");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final Clock clock;

    /**
     * Cancela un pedido NO pagado y devuelve las existencias al inventario.
     */
    @Transactional
    public OrderResponse cancel(Long userId, String orderNumber, boolean isAdmin) {
        Optional<PurchaseOrder> found = isAdmin
                ? orderRepository.findByOrderNumber(orderNumber)
                : orderRepository.findByOrderNumberAndUserId(orderNumber, userId);
        PurchaseOrder order = found.orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber));

        transition(order, OrderStatus.CANCELLED);
        restoreStock(order);

        log.info("Pedido {} cancelado; existencias devueltas ({} línea(s))", orderNumber, order.getItems().size());
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse ship(String orderNumber) {
        PurchaseOrder order = findByOrderNumber(orderNumber);
        transition(order, OrderStatus.SHIPPED);
        log.info("Pedido {} enviado", orderNumber);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse deliver(String orderNumber) {
        PurchaseOrder order = findByOrderNumber(orderNumber);
        transition(order, OrderStatus.DELIVERED);

        Instant deliveredAt = Instant.now(clock);
        order.setDeliveredAt(deliveredAt);

        boolean onTime = order.getEstimatedDeliveryDate() == null
                || !LocalDate.ofInstant(deliveredAt, clock.getZone()).isAfter(order.getEstimatedDeliveryDate());
        log.info("Pedido {} entregado {} (estimado {})", orderNumber, onTime ? "A TIEMPO" : "CON RETRASO",
                order.getEstimatedDeliveryDate());
        return orderMapper.toResponse(order);
    }

    /** Listado para el administrador: todos los pedidos, opcionalmente filtrados por estatus. */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> findAll(OrderStatus status, Pageable pageable) {
        validateSort(pageable.getSort());
        Page<PurchaseOrder> page = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(orderMapper::toSummary));
    }

    // ------------------------------------------------------------------

    private PurchaseOrder findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber));
    }

    private static void transition(PurchaseOrder order, OrderStatus target) {
        if (!order.getStatus().canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE,
                    "No se puede pasar el pedido " + order.getOrderNumber() + " de "
                            + order.getStatus() + " a " + target);
        }
        order.setStatus(target);
    }

    /** Bloquea los productos (mismo orden que al crear pedidos, sin deadlocks) y suma las cantidades. */
    private void restoreStock(PurchaseOrder order) {
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> products = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProduct().getId());
            product.setStock(product.getStock() + item.getQuantity());
        }
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order sortOrder : sort) {
            if (!SORTABLE_FIELDS.contains(sortOrder.getProperty())) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                        "No se puede ordenar por '" + sortOrder.getProperty() + "'. Campos permitidos: "
                                + String.join(", ", SORTABLE_FIELDS.stream().sorted().toList()));
            }
        }
    }
}