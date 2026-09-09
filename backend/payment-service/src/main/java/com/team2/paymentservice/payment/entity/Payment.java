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

    @Column(nullable = false)
    private String payerEmail; // 의뢰자 (결제자)

    @Column(nullable = false)
    private String payeeEmail; // 수리자 (정산 대상)

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer feeAmount;

    @Column(nullable = false)
    private Integer netAmount; // 수리자 정산액 (amount - feeAmount)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @Builder
    public Payment(Long postId, String payerEmail, String payeeEmail, Integer amount) {
        this.postId = postId;
        this.payerEmail = payerEmail;
        this.payeeEmail = payeeEmail;
        this.amount = amount;
        this.feeAmount = BigDecimal.valueOf(amount)
                .multiply(FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        this.netAmount = amount - this.feeAmount;
        this.status = PaymentStatus.COMPLETED;
        this.createdAt = LocalDateTime.now();
        this.paidAt = LocalDateTime.now();
    }
}
