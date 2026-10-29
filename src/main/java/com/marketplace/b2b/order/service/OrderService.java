package com.marketplace.b2b.order.service;

import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import com.marketplace.b2b.order.dto.OrderCreationResult;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.dto.OrderSummaryResponse;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.mapper.OrderMapper;
import com.marketplace.b2b.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Punto de entrada de pedidos. Aplica la idempotencia ALREDEDOR de la transacción de creación:
 * así, si la BD rechaza un duplicado, la transacción fallida ya terminó y se puede leer el pedido original.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9_-]{8,100}$");
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "totalAmount", "status");

    private final OrderPlacementService placementService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderCreationResult createOrder(Long userId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);

        // Reintento normal: la llave ya se usó → se devuelve el mismo pedido
        Optional<PurchaseOrder> existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            log.info("Petición repetida con Idempotency-Key; se devuelve el pedido folio={}",
                    existing.get().getOrderNumber());
            return replay(existing.get());
        }

        try {
            PurchaseOrder created = placementService.place(userId, idempotencyKey);
            return new OrderCreationResult(orderMapper.toResponse(created), true);
        } catch (DataIntegrityViolationException ex) {
            // Carrera: dos peticiones simultáneas con la misma llave; la otra ganó
            log.warn("Pedido concurrente con la misma Idempotency-Key detectado por la BD");
            return orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .map(this::replay)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, String orderNumber, boolean isAdmin) {
        // Un cliente solo ve sus pedidos; el ADMIN ve cualquiera
        Optional<PurchaseOrder> order = isAdmin
                ? orderRepository.findByOrderNumber(orderNumber)
                : orderRepository.findByOrderNumberAndUserId(orderNumber, userId);
        return order.map(orderMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(Long userId, Pageable pageable) {
        validateSort(pageable.getSort());
        return PageResponse.from(orderRepository.findByUserId(userId, pageable).map(orderMapper::toSummary));
    }

    // ------------------------------------------------------------------

    private OrderCreationResult replay(PurchaseOrder order) {
        return new OrderCreationResult(orderMapper.toResponse(order), false);
    }

    private static void validateIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                    "El header Idempotency-Key debe tener entre 8 y 100 caracteres (letras, números, '-' o '_')");
        }
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                        "No se puede ordenar por '" + order.getProperty() + "'. Campos permitidos: "
                                + String.join(", ", SORTABLE_FIELDS.stream().sorted().toList()));
            }
        }
    }
}
