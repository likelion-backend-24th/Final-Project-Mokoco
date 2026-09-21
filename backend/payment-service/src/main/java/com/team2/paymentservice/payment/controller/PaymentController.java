package com.team2.paymentservice.payment.controller;

import com.team2.common.security.LoginUser;
import com.team2.paymentservice.payment.dto.*;
import com.team2.paymentservice.payment.entity.PaymentOrder;
import com.team2.paymentservice.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    public record Prepare(Long postId) {}
    public record OrderResponse(String paymentId, Long postId, int baseAmount, int totalAmount, String payeeEmail) {}
    @PostMapping("/prepare")
    public OrderResponse prepare(@RequestBody Prepare request, @AuthenticationPrincipal LoginUser user) {
        PaymentOrder order = paymentService.prepare(request.postId(), user);
        return new OrderResponse(order.getPaymentId(), order.getPostId(), order.getBaseAmount(), order.getTotalAmount(), order.getPayeeEmail());
    }
    @PostMapping
    public ResponseEntity<Long> createPayment(@RequestBody PaymentRequestDto.Create request, @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(paymentService.createPayment(request, user));
    }
    @GetMapping("/{paymentId}")
    public PaymentResponseDto getPayment(@PathVariable Long paymentId, @AuthenticationPrincipal LoginUser user) {
        return paymentService.getPayment(paymentId, user.email());
    }
    @GetMapping("/post/{postId}")
    public PaymentResponseDto getPaymentByPostId(@PathVariable Long postId, @AuthenticationPrincipal LoginUser user) {
        return paymentService.getPaymentByPostId(postId, user.email());
    }
}
