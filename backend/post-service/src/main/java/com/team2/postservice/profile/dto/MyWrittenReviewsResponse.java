package com.team2.postservice.profile.dto;

import com.team2.postservice.review.dto.ReviewResponseDto;

import java.util.List;

public record MyWrittenReviewsResponse(
        long totalCount,
        List<ReviewResponseDto> reviews
) {
}
