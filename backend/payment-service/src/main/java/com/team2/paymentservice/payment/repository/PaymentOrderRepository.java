package com.team2.paymentservice.payment.repository;

import com.team2.paymentservice.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, String> {
    Optional<PaymentOrder> findByPostId(Long postId);
}
