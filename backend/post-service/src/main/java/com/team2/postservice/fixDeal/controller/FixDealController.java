package com.team2.postservice.fixDeal.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.fixDeal.dto.FixDealDetailResponse;
import com.team2.postservice.fixDeal.dto.FixDealStatusResponse;
import com.team2.postservice.fixDeal.service.FixDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FixDealController {

    private final FixDealService fixDealService;

    // 거래 진행 상태 전이는 전부 ContractService.advance()(계약서 페이지)가 담당한다.
    // 이 컨트롤러는 읽기 전용 조회만 제공한다.

    @GetMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<FixDealDetailResponse> getFixDeal(@PathVariable Long fixDealId,
                                                             @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(fixDealService.getFixDeal(fixDealId, user.email()));
    }

    @GetMapping("/posts/{postId}/fix-deal")
    public ResponseEntity<FixDealStatusResponse> getStatusByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(fixDealService.getStatusByPostId(postId));
    }
}
