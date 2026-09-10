package com.team2.paymentservice.payment.repository;

import com.team2.paymentservice.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPostId(Long postId);
    boolean existsByPostId(Long postId);
    boolean existsByPortonePaymentId(String portonePaymentId);
}
