package com.team2.paymentservice.payment.entity;

import com.team2.common.payment.PaymentContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDateTime;
import java.util.UUID;

// 결제창을 열기 전에 서버가 먼저 만들어두는 주문 — postId/금액/수신자를 여기 담아두고,
// 결제 확정(confirm)과 웹훅 둘 다 이 레코드를 유일한 진실 소스로 삼는다. 클라이언트가 결제창에
// 실어보내는 customData는 더 이상 신뢰하지 않는다.
@Entity
@Table(name = "payment_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrder {
    @Id
    @Column(length = 64)
    private String paymentId;

    // 취소 후 재결제 시 새 주문을 또 만들 수 있어야 하므로 더 이상 unique가 아니다.
    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long fixDealId;

    @Column(nullable = false)
    private Long payerId;

    @Column(nullable = false)
    private String payerEmail;

    @Column(nullable = false)
    private String payeeEmail;

    @Column(nullable = false)
    private int baseAmount;

    // release 모델은 의뢰자가 견적 금액(baseAmount) 그대로 결제하고(수수료를 얹지 않음), 플랫폼
    // 수수료는 수리자 정산액(Payment.netAmount)에서 차감한다 — 그래서 totalAmount == baseAmount.
    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PaymentOrder(PaymentContext context) {
        if (context.baseAmount() <= 0) throw new IllegalArgumentException("baseAmount must be positive");
        paymentId = "payment-" + UUID.randomUUID();
        postId = context.postId();
        fixDealId = context.fixDealId();
        payerId = context.payerId();
        payerEmail = context.payerEmail();
        payeeEmail = context.payeeEmail();
        baseAmount = context.baseAmount();
        totalAmount = context.baseAmount();
        createdAt = LocalDateTime.now();
    }
}
