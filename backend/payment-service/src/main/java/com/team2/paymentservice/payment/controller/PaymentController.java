package com.team2.paymentservice.payment.controller;

import com.team2.paymentservice.payment.dto.PayeePaymentsResponseDto;
import com.team2.paymentservice.payment.dto.PaymentRequestDto;
import com.team2.paymentservice.payment.dto.PaymentResponseDto;
import com.team2.paymentservice.payment.entity.PaymentOrder;
import com.team2.paymentservice.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final int MAX_PAGE_SIZE = 50;

    private final PaymentService paymentService;

    public record Prepare(Long postId) {}
    public record OrderResponse(String paymentId, Long postId, int baseAmount, int totalAmount, String payeeEmail) {}

    // 결제창을 열기 전에 서버가 postId만 보고 금액·수신자를 직접 조회해 주문을 미리 만든다.
    // PortOne 결제창엔 여기서 발급한 paymentId/totalAmount만 사용 — 클라이언트가 금액을 정하지 않는다.
    @PostMapping("/prepare")
    public OrderResponse prepare(@RequestBody Prepare request, @RequestHeader("X-User-Email") String payerEmail) {
        PaymentOrder order = paymentService.prepare(request.postId(), payerEmail);
        return new OrderResponse(order.getPaymentId(), order.getPostId(), order.getBaseAmount(), order.getTotalAmount(), order.getPayeeEmail());
    }

    // 결제 확정 (의뢰자) — prepare로 만든 주문을 paymentId로 찾아 PortOne 실제 결제와 대조한다.
    @PostMapping
    public ResponseEntity<Long> createPayment(@RequestBody PaymentRequestDto.Create request,
                                               @RequestHeader("X-User-Email") String payerEmail) {
        Long paymentId = paymentService.createPayment(request, payerEmail);
        return ResponseEntity.ok(paymentId);
    }

    // 수리자 본인이 받은/받을 정산 내역 (실제 송금 없음, settledAt 여부만 표시)
    @GetMapping("/mine")
    public ResponseEntity<PayeePaymentsResponseDto> getMyPayments(
            @RequestHeader("X-User-Email") String payeeEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(safeSize, 1));
        return ResponseEntity.ok(paymentService.getMyPayments(payeeEmail, pageable));
    }

    // 결제 단건 조회 (결제자 또는 수리자 본인만)
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable Long paymentId,
                                                          @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId, userEmail));
    }

    // 게시글(수리 요청) 기준 결제 조회 (프론트가 계약서 페이지에서 결제 상태 확인용으로 호출)
    @GetMapping("/post/{postId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByPostId(@PathVariable Long postId,
                                                                  @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(paymentService.getPaymentByPostId(postId, userEmail));
    }
}
