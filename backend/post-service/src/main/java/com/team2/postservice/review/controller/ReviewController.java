package com.team2.postservice.review.controller;

import com.team2.postservice.review.dto.ReviewRequestDto;
import com.team2.postservice.review.dto.ReviewResponseDto;
import com.team2.postservice.review.dto.UserReviewsResponseDto;
import com.team2.postservice.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewService reviewService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createReview(@RequestPart("review") ReviewRequestDto.Create request,
                                              @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                              @RequestHeader("X-User-Email") String reviewerEmail) {
        Long reviewId = reviewService.createReview(request, images, reviewerEmail);
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

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReview(reviewId));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByPostId(@RequestParam Long postId) {
        return ResponseEntity.ok(reviewService.existsByPostId(postId));
    }
}