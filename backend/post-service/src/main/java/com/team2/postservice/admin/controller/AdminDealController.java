package com.team2.postservice.admin.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.fixDeal.dto.AdminDealResponse;
import com.team2.postservice.fixDeal.service.FixDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 진행 중인 거래를 관리자가 한눈에 보는 현황판. 참가자 본인 확인 없이 전체를 볼 수 있어야 하므로
// 일반 FixDealController와 분리한다 — 권한은 FixDealService.listDealsForAdmin이 매 요청 다시 확인한다.
@RestController
@RequestMapping("/api/admin/deals")
@RequiredArgsConstructor
public class AdminDealController {

    private static final int MAX_PAGE_SIZE = 50;

    private final FixDealService fixDealService;

    @GetMapping
    public ResponseEntity<Page<AdminDealResponse>> listDeals(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return ResponseEntity.ok(fixDealService.listDealsForAdmin(user, status, pageable));
    }
}
