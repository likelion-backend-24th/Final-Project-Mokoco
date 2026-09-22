package com.team2.paymentservice.payment.repository;

import com.team2.paymentservice.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPostId(Long postId);
    boolean existsByPostId(Long postId);
    boolean existsByPortonePaymentId(String portonePaymentId);

    // 내 정산내역 목록: 결제자(의뢰자) 또는 수신자(수리자)로 걸린 건을 최신순으로 조회
    List<Payment> findByPayerEmailOrPayeeEmailOrderByCreatedAtDesc(String payerEmail, String payeeEmail);
}
