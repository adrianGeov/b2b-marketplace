package com.marketplace.b2b.order.service;

import com.marketplace.b2b.cart.entity.Cart;
import com.marketplace.b2b.cart.entity.CartItem;
import com.marketplace.b2b.cart.repository.CartRepository;
import com.marketplace.b2b.catalog.entity.Product;
import com.marketplace.b2b.catalog.repository.ProductRepository;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.order.entity.OrderItem;
import com.marketplace.b2b.order.entity.OrderStatus;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import com.marketplace.b2b.order.repository.OrderRepository;
import com.marketplace.b2b.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Crea el pedido a partir del carrito en UNA transacción:
 * bloquea productos → valida → descuenta existencias → congela precios → guarda → vacía carrito.
 * Si algo falla, se revierte todo (no quedan existencias descontadas sin pedido).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPlacementService {

    static final int ESTIMATED_DELIVERY_DAYS = 3;
    private static final DateTimeFormatter FOLIO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public PurchaseOrder place(Long userId, String idempotencyKey) {
        // 1. Ids de los productos del carrito (sin cargarlos todavía)
        List<Long> productIds = cartRepository.findProductIdsByUserId(userId);
        if (productIds.isEmpty()) {
            throw emptyCart();
        }

        // 2. Bloqueo pesimista de esos productos ANTES de leer sus existencias
        Map<Long, Product> lockedProducts = productRepository.findAllByIdForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 3. Ahora sí se carga el carrito (sus productos ya están en memoria, actualizados y bloqueados)
        Cart cart = cartRepository.findByUserId(userId)
                .filter(found -> !found.getItems().isEmpty())
                .orElseThrow(OrderPlacementService::emptyCart);

        // 4. Validar TODO antes de modificar nada
        cart.getItems().forEach(item -> validate(lockedProducts.get(item.getProduct().getId()), item));

        // 5. Construir el pedido: descontar existencias y congelar precios
        LocalDate today = LocalDate.now(clock);
        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(generateOrderNumber(today))
                .user(userRepository.getReferenceById(userId))
                .status(OrderStatus.PENDING_PAYMENT)
                .idempotencyKey(idempotencyKey)
                .estimatedDeliveryDate(today.plusDays(ESTIMATED_DELIVERY_DAYS))
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            int quantity = cartItem.getQuantity();

            product.setStock(product.getStock() - quantity);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            order.addItem(OrderItem.builder()
                    .product(product)
                    .sku(product.getSku())
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);

        // 6. saveAndFlush: si otra petición ya usó la misma llave, el UNIQUE falla AQUÍ
        PurchaseOrder saved = orderRepository.saveAndFlush(order);
        cart.clear();

        log.info("Pedido creado folio={} usuarioId={} total={} lineas={}",
                saved.getOrderNumber(), userId, saved.getTotalAmount(), saved.getItems().size());
        return saved;
    }

    private static void validate(Product product, CartItem item) {
        if (product == null || !product.isActive()) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE,
                    "El producto " + item.getProduct().getSku() + " ya no está disponible; retírelo del carrito");
        }
        if (product.getStock() < item.getQuantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Existencias insuficientes para el SKU " + product.getSku()
                            + ": disponibles " + product.getStock() + ", solicitadas " + item.getQuantity());
        }
    }

    /** Ejemplo: PED-20261028-8F3K2A */
    private static String generateOrderNumber(LocalDate date) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "PED-" + date.format(FOLIO_DATE) + "-" + random;
    }

    private static BusinessException emptyCart() {
        return new BusinessException(ErrorCode.EMPTY_CART,
                "El carrito está vacío; agregue productos antes de crear el pedido");
    }
}