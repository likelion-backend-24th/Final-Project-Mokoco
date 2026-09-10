package com.team2.paymentservice.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    private static final BigDecimal FEE_RATE = BigDecimal.valueOf(0.10);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long postId;

    // PortOne이 채번한 결제 건 식별자. 프론트가 결제창 호출 시 생성한 값과 동일하다.
    // 프론트 확인 요청과 웹훅이 같은 결제를 중복 저장하지 않도록 이 값으로 멱등성을 보장한다.
    @Column(nullable = false, unique = true)
    private String portonePaymentId;

    @Column(nullable = false)
    private String payerEmail; // 의뢰자 (결제자)

    @Column(nullable = false)
    private String payeeEmail; // 수리자 (정산 대상)

    @Column(nullable = false)
    private Integer amount; // 의뢰자가 실제 결제한 총액 (baseAmount + feeAmount)

    @Column(nullable = false)
    private Integer feeAmount; // 플랫폼 수수료 (baseAmount의 10%, 의뢰자가 추가로 부담)

    @Column(nullable = false)
    private Integer netAmount; // 수리자가 받는 금액 = 수리자가 제안한 금액(baseAmount) 그대로

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @Builder
    public Payment(Long postId, String portonePaymentId, String payerEmail, String payeeEmail,
                   Integer totalAmount, Integer baseAmount) {
        this.postId = postId;
        this.portonePaymentId = portonePaymentId;
        this.payerEmail = payerEmail;
        this.payeeEmail = payeeEmail;
        this.amount = totalAmount;
        this.netAmount = baseAmount;
        this.feeAmount = totalAmount - baseAmount;
        this.status = PaymentStatus.COMPLETED;
        this.createdAt = LocalDateTime.now();
        this.paidAt = LocalDateTime.now();
    }

    public static int calculateTotalAmount(int baseAmount) {
        int fee = BigDecimal.valueOf(baseAmount)
                .multiply(FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return baseAmount + fee;
    }
}
