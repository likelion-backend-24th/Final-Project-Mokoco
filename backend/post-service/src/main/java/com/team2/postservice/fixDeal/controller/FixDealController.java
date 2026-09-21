package com.team2.postservice.fixDeal.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.team2.common.security.LoginUser;
import com.team2.postservice.fixDeal.dto.FixDealDetailResponse;
import com.team2.postservice.fixDeal.dto.FixDealStatusResponse;
import com.team2.postservice.fixDeal.service.FixDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FixDealController {

    private final FixDealService fixDealService;

    @PatchMapping("/fix-deals/{fixDealId}/product-sent")
    public ResponseEntity<Void> markProductSent(@PathVariable Long fixDealId,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        fixDealService.markProductSent(fixDealId, loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/fix-deals/{fixDealId}/repairing")
    public ResponseEntity<Void> markRepairing(@PathVariable Long fixDealId,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        fixDealService.markRepairing(fixDealId, loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<FixDealDetailResponse> getFixDeal(@PathVariable Long fixDealId,
                                                             @AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(fixDealService.getFixDeal(fixDealId, loginUser.userId()));
    }

    @PatchMapping("/fix-deals/{fixDealId}/repair-done")
    public ResponseEntity<Void> requestCompletion(@PathVariable Long fixDealId,
                                                   @AuthenticationPrincipal LoginUser loginUser) {
        fixDealService.requestCompletion(fixDealId, loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/fix-deals/{fixDealId}/complete")
    public ResponseEntity<Void> acceptCompletion(@PathVariable Long fixDealId,
                                                  @AuthenticationPrincipal LoginUser loginUser) {
        fixDealService.acceptCompletion(fixDealId, loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/posts/{postId}/fix-deal")
    public ResponseEntity<FixDealStatusResponse> getStatusByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(fixDealService.getStatusByPostId(postId));
    }
}
