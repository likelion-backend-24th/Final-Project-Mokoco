package com.team2.postservice.contract.dto;

public record PaymentSummary(String status, Integer amount, Integer feeAmount, Integer netAmount, boolean settled) {}
