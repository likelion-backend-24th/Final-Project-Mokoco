package com.team2.common.payment;

// post-service가 결제 대상 거래의 진실을 담아 payment-service에 내려주는 응답.
// payment-service는 이 값을 유일한 진실 소스로 삼고, 클라이언트가 보낸 금액/수신자는 신뢰하지 않는다.
public record PaymentContext(Long postId, Long fixDealId, Long payerId, String payerEmail,
                              Long payeeId, String payeeEmail, int baseAmount, String status) {}
