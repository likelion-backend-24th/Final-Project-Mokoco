package com.team2.postservice.review.controller;

import com.team2.postservice.review.dto.ReviewRequestDto;
import com.team2.postservice.review.dto.UserReviewsResponseDto;
import com.team2.postservice.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Long> createReview(@RequestBody ReviewRequestDto.Create request,
                                              @RequestHeader("X-User-Email") String reviewerEmail) {
        Long reviewId = reviewService.createReview(request, reviewerEmail);
        return ResponseEntity.ok(reviewId);
    }

    @GetMapping
    public ResponseEntity<UserReviewsResponseDto> getUserReviews(
            @RequestParam String revieweeEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(safeSize, 1));
        return ResponseEntity.ok(reviewService.getUserReviews(revieweeEmail, pageable));
    }
}
