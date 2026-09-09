package com.team2.paymentservice.payment.controller;

import com.team2.paymentservice.payment.dto.PaymentRequestDto;
import com.team2.paymentservice.payment.dto.PaymentResponseDto;
import com.team2.paymentservice.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 생성 (의뢰자)
    @PostMapping
    public ResponseEntity<Long> createPayment(@RequestBody PaymentRequestDto.Create request,
                                               @RequestHeader("X-User-Email") String payerEmail) {
        Long paymentId = paymentService.createPayment(request, payerEmail);
        return ResponseEntity.ok(paymentId);
    }

    // 결제 단건 조회 (결제자 또는 수리자 본인만)
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable Long paymentId,
                                                          @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId, userEmail));
    }

    // 게시글(수리 요청) 기준 결제 조회
    @GetMapping("/post/{postId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByPostId(@PathVariable Long postId,
                                                                  @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(paymentService.getPaymentByPostId(postId, userEmail));
    }
}
