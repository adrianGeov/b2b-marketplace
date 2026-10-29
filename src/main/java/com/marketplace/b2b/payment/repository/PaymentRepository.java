package com.marketplace.b2b.payment.repository;

import com.marketplace.b2b.payment.entity.Payment;
import com.marketplace.b2b.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "order")
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @EntityGraph(attributePaths = "order")
    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, PaymentStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}