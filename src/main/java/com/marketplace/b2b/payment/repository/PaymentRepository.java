package com.marketplace.b2b.payment.repository;

import com.marketplace.b2b.payment.entity.Payment;
import com.marketplace.b2b.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}