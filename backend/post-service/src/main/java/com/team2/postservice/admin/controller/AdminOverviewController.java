package com.team2.postservice.admin.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.fixDeal.dto.AdminOverviewResponse;
import com.team2.postservice.fixDeal.service.FixDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 사이드바 첫 화면(대시보드 개요) — 회원/거래/신고 핵심 지표를 한 화면에 모아 보여준다.
@RestController
@RequestMapping("/api/admin/overview")
@RequiredArgsConstructor
public class AdminOverviewController {

    private final FixDealService fixDealService;

    @GetMapping
    public ResponseEntity<AdminOverviewResponse> getOverview(@AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(fixDealService.getOverview(user));
    }
}
