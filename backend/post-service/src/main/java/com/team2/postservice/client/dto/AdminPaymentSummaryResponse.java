package com.team2.postservice.client.dto;

// payment-service의 관리자 거래 현황판 요약 응답.
public record AdminPaymentSummaryResponse(long totalCompletedAmount, long totalSettledAmount) {
}
