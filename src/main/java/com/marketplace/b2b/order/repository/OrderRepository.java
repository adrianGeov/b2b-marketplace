package com.marketplace.b2b.order.repository;

import com.marketplace.b2b.order.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /** Idempotencia: si ya existe, se devuelve el mismo pedido en lugar de crear otro. */
    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findByOrderNumberAndUserId(String orderNumber, Long userId);

    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    Page<PurchaseOrder> findByUserId(Long userId, Pageable pageable);
}