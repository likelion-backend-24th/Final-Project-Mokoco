package com.team2.paymentservice.payment.dto;

public class PaymentRequestDto {

    // post-service에 '완료 신청' 상태·채택된 제안 조회 계약이 생기면
    // payeeEmail도 post-service에서 직접 조회해 검증하도록 교체한다.
    public record Create(Long postId, String payeeEmail, Integer amount) {}
}
