package com.team2.paymentservice.payment.entity;

import com.team2.common.payment.PaymentContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrder {
    @Id @Column(length = 64) private String paymentId;
    @Column(nullable = false, unique = true) private Long postId;
    @Column(nullable = false) private Long fixDealId;
    @Column(nullable = false) private Long payerId;
    @Column(nullable = false) private Long payeeId;
    @Column(nullable = false) private int baseAmount;
    @Column(nullable = false) private int totalAmount;
    @Column(nullable = false) private LocalDateTime createdAt;

    public PaymentOrder(PaymentContext context) {
        paymentId = "payment-" + UUID.randomUUID();
        postId = context.postId(); fixDealId = context.fixDealId(); payerId = context.payerId();
        payeeId = context.payeeId();
        baseAmount = context.baseAmount(); totalAmount = Payment.calculateTotalAmount(baseAmount);
        createdAt = LocalDateTime.now();
    }
}
