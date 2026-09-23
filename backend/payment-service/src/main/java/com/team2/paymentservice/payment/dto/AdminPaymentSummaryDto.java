package com.team2.paymentservice.payment.dto;

// 관리자 거래 현황판 요약 카드용 — 전체 결제를 통틀어 집계한다.
public record AdminPaymentSummaryDto(long totalCompletedAmount, long totalSettledAmount) {
}
