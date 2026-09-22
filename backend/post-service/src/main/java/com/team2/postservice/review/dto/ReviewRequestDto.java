package com.team2.postservice.review.dto;

public class ReviewRequestDto {
    public record Create(Long postId, Integer rating, String content) {}
}
