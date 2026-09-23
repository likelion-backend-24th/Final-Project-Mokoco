package com.team2.paymentservice.payment.controller;

import com.team2.paymentservice.payment.dto.AdminPaymentSummaryDto;
import com.team2.paymentservice.payment.dto.PaymentResponseDto;
import com.team2.paymentservice.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// post-service가 결제 상태를 확인하거나(작업 시작 게이팅) 정산을 확정할 때(거래 완료) 호출하는
// 서비스 간 전용 API. 인증은 SecurityConfig의 internalServiceSecurityFilterChain(/internal/**)이
// X-Internal-Service-Key로 처리하므로 여기선 별도 이메일 검증이 필요 없다.
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/post/{postId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(paymentService.internalPayment(postId));
    }

    // 관리자 거래 현황판 요약 카드(거래 완료 금액/정산된 금액)용.
    @GetMapping("/admin/summary")
    public ResponseEntity<AdminPaymentSummaryDto> getAdminSummary() {
        return ResponseEntity.ok(paymentService.getAdminSummary());
    }

    // 거래 완료(COMPLETED) 시 post-service가 호출하는 정산 확정.
    // 실제 송금은 하지 않고 settledAt만 기록한다 (Payment.settle() 주석 참고).
    @PostMapping("/post/{postId}/settle")
    public ResponseEntity<Void> settlePayment(@PathVariable Long postId) {
        paymentService.settlePayment(postId);
        return ResponseEntity.ok().build();
    }
}
