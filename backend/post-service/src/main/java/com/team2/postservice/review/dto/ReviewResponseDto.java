package com.team2.postservice.review.dto;

import com.team2.postservice.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponseDto(
        Long id,
        Long postId,
        String reviewerEmail,
        String revieweeEmail,
        Integer rating,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponseDto from(Review review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getPostId(),
                review.getReviewerEmail(),
                review.getRevieweeEmail(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
