package com.team2.paymentservice.payment.repository;

import com.team2.paymentservice.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, String> {
    // 취소 후 재결제로 한 글에 여러 주문이 있을 수 있어, 재사용 가능한 주문인지 판단할 때는 최신 것만 본다.
    Optional<PaymentOrder> findFirstByPostIdOrderByCreatedAtDesc(Long postId);
}
