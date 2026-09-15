package com.team2.postservice.review.dto;

import com.team2.postservice.review.entity.Review;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponseDto(
        Long id,
        Long postId,
        String reviewerEmail,
        String revieweeEmail,
        Integer rating,
        String content,
        List<String> imageUrls,
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
                review.getImages().stream()
                        .sorted((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()))
                        .map(image -> image.getImageUrl())
                        .toList(),
                review.getCreatedAt()
        );
    }
}
