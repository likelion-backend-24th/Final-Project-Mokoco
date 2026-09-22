package com.team2.paymentservice.payment.dto;

public class PaymentRequestDto {

    public record Create(Long postId, Long payeeId, Integer amount, Integer baseAmount, String paymentId) {}
}
