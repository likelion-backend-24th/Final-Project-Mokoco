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

    // 취소 후 재결제를 허용하므로 한 글에 여러 행(취소된 것 + 새로 결제된 것)이 있을 수 있어 더 이상 unique가 아니다.
    @Column(nullable = false)
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
    private Integer amount; // 의뢰자가 실제 결제한 총액 = 견적 금액(baseAmount) 그대로

    @Column(nullable = false)
    private Integer feeAmount; // 플랫폼 수수료 (견적 금액의 10%, 수리자 정산액에서 차감)

    @Column(nullable = false)
    private Integer netAmount; // 수리자가 받는 금액 = 견적 금액 - 플랫폼 수수료(10%)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    // 거래가 COMPLETED 되어 플랫폼이 수리자 몫을 '정산 확정'했음을 기록하는 장부용 타임스탬프.
    // 실제 PG 에스크로 해제나 수리자 계좌 송금 연동은 이번 범위 밖이며, 이 필드는 그 지점을
    // 표시만 해둔다 — 나중에 실제 지급 연동을 붙일 때 settle() 호출부가 그 자리다.
    private LocalDateTime settledAt;

    @Builder
    public Payment(Long postId, String portonePaymentId, String payerEmail, String payeeEmail,
                   Integer totalAmount, Integer baseAmount) {
        this.postId = postId;
        this.portonePaymentId = portonePaymentId;
        this.payerEmail = payerEmail;
        this.payeeEmail = payeeEmail;
        // 의뢰자는 견적 금액(baseAmount) 그대로 결제한다 — 수수료를 얹어 더 받지 않는다.
        this.amount = totalAmount;
        this.feeAmount = calculateFee(baseAmount);
        this.netAmount = baseAmount - this.feeAmount;
        this.status = PaymentStatus.COMPLETED;
        this.createdAt = LocalDateTime.now();
        this.paidAt = LocalDateTime.now();
    }

    public void settle() {
        if (settledAt == null) settledAt = LocalDateTime.now();
    }

    // PortOne에서 결제가 취소/환불된 것을 웹훅으로 확인했을 때 호출한다. 이미 정산 확정된(settledAt이
    // 찍힌) 결제를 취소하는 흐름은 이번 범위 밖이라 별도로 막지 않는다 — 정산 이후 취소는 운영에서
    // 수동으로 처리한다.
    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }

    // 플랫폼 수수료 = 견적 금액의 10% (수리자에게 정산될 때 이 금액만큼 차감된다)
    public static int calculateFee(int baseAmount) {
        return BigDecimal.valueOf(baseAmount)
                .multiply(FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
