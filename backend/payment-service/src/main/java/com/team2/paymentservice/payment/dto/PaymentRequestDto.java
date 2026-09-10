package com.team2.paymentservice.payment.dto;

public class PaymentRequestDto {

    public record Create(Long postId, String payeeEmail, Integer amount, Integer baseAmount, String paymentId) {}
}
