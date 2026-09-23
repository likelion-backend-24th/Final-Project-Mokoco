package com.team2.postservice.admin.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.admin.dto.AdminPaymentListResponse;
import com.team2.postservice.fixDeal.service.FixDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 관리자 결제/정산 상세 내역 — payment-service를 그대로 조회해서 보여준다(집계는 거래 현황판 요약 API).
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private static final int MAX_PAGE_SIZE = 50;

    private final FixDealService fixDealService;

    @GetMapping
    public ResponseEntity<AdminPaymentListResponse> listPayments(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return ResponseEntity.ok(fixDealService.listPaymentsForAdmin(user, status, Math.max(page, 0), safeSize));
    }
}
