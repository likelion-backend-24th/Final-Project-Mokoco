package com.team2.postservice.review.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.review.dto.ReviewRequestDto;
import com.team2.postservice.review.dto.ReviewResponseDto;
import com.team2.postservice.review.dto.UserReviewsResponseDto;
import com.team2.postservice.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewService reviewService;

    // 이미지 업로드가 껴 있어 게이트웨이를 우회해 post-service로 직결되는 엔드포인트라,
    // 클라이언트가 실어 보내는 헤더를 그대로 믿지 않고 Authorization Bearer 토큰을 직접 검증한다.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createReview(@RequestPart("review") ReviewRequestDto.Create request,
                                              @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                              @AuthenticationPrincipal LoginUser user) {
        Long reviewId = reviewService.createReview(request, images, user.email());
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

    @GetMapping("/by-post")
    public ResponseEntity<ReviewResponseDto> getReviewByPostId(@RequestParam Long postId) {
        return ResponseEntity.ok(reviewService.getReviewByPostId(postId));
    }
}
