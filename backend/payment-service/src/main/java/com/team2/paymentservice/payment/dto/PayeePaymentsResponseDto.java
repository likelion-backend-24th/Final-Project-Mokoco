package com.team2.paymentservice.payment.dto;

import java.util.List;

public record PayeePaymentsResponseDto(
        Integer settledAmount,  // 정산 확정되어 받을 금액 합계
        Integer pendingAmount,  // 거래 완료 전이라 아직 정산 대기 중인 금액 합계
        long totalCount,
        List<PaymentResponseDto> payments
) {
}
