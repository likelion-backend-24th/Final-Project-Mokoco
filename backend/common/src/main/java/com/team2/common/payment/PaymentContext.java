package com.team2.common.payment;

public record PaymentContext(Long postId, Long fixDealId, Long payerId, String payerEmail,
                             String payeeEmail, int baseAmount, String status) {}
