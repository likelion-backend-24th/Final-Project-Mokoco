package com.team2.postservice.profile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.team2.common.security.LoginUser;
import com.team2.postservice.profile.dto.MyWrittenReviewsResponse;
import com.team2.postservice.profile.dto.TransactionHistoryResponse;
import com.team2.postservice.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProfileService profileService;

    @GetMapping("/transactions")
    public ResponseEntity<TransactionHistoryResponse> getMyTransactions(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "requester") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return ResponseEntity.ok(profileService.getMyTransactions(loginUser.userId(), role, pageable));
    }

    @GetMapping("/reviews")
    public ResponseEntity<MyWrittenReviewsResponse> getMyWrittenReviews(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return ResponseEntity.ok(profileService.getMyWrittenReviews(loginUser.userId(), pageable));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<TransactionHistoryResponse> getTransactionsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "repairer") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return ResponseEntity.ok(profileService.getMyTransactions(userId, role, pageable));
    }
}
