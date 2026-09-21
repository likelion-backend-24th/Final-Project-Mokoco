package com.team2.common.payment;

public record PaymentContext(Long postId, Long fixDealId, Long payerId, Long payeeId, int baseAmount, String status) {}
