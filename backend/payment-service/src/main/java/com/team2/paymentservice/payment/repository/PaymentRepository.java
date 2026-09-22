package com.team2.paymentservice.payment.repository;

import com.team2.paymentservice.payment.entity.Payment;
import com.team2.paymentservice.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 취소 후 재결제로 한 글에 여러 행이 있을 수 있어, "현재 유효한" 결제는 항상 최신 행을 본다.
    Optional<Payment> findFirstByPostIdOrderByCreatedAtDesc(Long postId);
    Optional<Payment> findByPortonePaymentId(String portonePaymentId);
    boolean existsByPostIdAndStatus(Long postId, PaymentStatus status);
    boolean existsByPortonePaymentId(String portonePaymentId);

    Page<Payment> findByPayeeEmailOrderByCreatedAtDesc(String payeeEmail, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.netAmount), 0) FROM Payment p WHERE p.payeeEmail = :payeeEmail AND p.settledAt IS NOT NULL")
    int sumSettledNetAmount(@Param("payeeEmail") String payeeEmail);

    @Query("SELECT COALESCE(SUM(p.netAmount), 0) FROM Payment p WHERE p.payeeEmail = :payeeEmail AND p.settledAt IS NULL")
    int sumPendingNetAmount(@Param("payeeEmail") String payeeEmail);
}
