package com.team2.postservice.post.dto;

public record PaymentSummary(String status, Integer amount, Integer feeAmount, Integer netAmount, boolean settled) {}
