package com.team2.paymentservice.payment.client;

public record PostInfoResponse(
        Long id,
        String authorEmail,
        String status
) {
}
