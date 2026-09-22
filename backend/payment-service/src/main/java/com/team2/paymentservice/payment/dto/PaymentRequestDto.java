package com.team2.paymentservice.payment.dto;

public class PaymentRequestDto {

    // 금액/수신자는 더 이상 클라이언트에서 받지 않는다 — /payments/prepare로 미리 만들어둔
    // PaymentOrder(paymentId로 식별)가 유일한 진실 소스다.
    public record Create(String paymentId, Long postId) {}
}
