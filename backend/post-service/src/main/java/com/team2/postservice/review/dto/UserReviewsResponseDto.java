package com.team2.postservice.review.dto;

import java.util.List;

public record UserReviewsResponseDto(
        Double averageRating,
        Long totalCount,
        List<ReviewResponseDto> reviews
) {
}
