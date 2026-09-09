package com.team2.postservice.fixDeal.controller;

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
                                                 @RequestHeader("X-User-Email") String repairerEmail) {
        fixDealService.markProductSent(fixDealId, repairerEmail);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/fix-deals/{fixDealId}/repairing")
    public ResponseEntity<Void> markRepairing(@PathVariable Long fixDealId,
                                               @RequestHeader("X-User-Email") String repairerEmail) {
        fixDealService.markRepairing(fixDealId, repairerEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<FixDealDetailResponse> getFixDeal(@PathVariable Long fixDealId,
                                                             @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(fixDealService.getFixDeal(fixDealId, userEmail));
    }

    @PatchMapping("/fix-deals/{fixDealId}/repair-done")
    public ResponseEntity<Void> requestCompletion(@PathVariable Long fixDealId,
                                                   @RequestHeader("X-User-Email") String repairerEmail) {
        fixDealService.requestCompletion(fixDealId, repairerEmail);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/fix-deals/{fixDealId}/complete")
    public ResponseEntity<Void> acceptCompletion(@PathVariable Long fixDealId,
                                                  @RequestHeader("X-User-Email") String requesterEmail) {
        fixDealService.acceptCompletion(fixDealId, requesterEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/posts/{postId}/fix-deal")
    public ResponseEntity<FixDealStatusResponse> getStatusByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(fixDealService.getStatusByPostId(postId));
    }
}
