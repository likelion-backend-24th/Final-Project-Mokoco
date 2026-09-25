package com.team2.postservice.review.dto;

import com.team2.postservice.review.entity.Review;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponseDto(
        Long id,
        Long postId,
        String reviewerEmail,
        String revieweeEmail,
        // 목록 화면에서 이메일 대신 닉네임을 보여주려고 쓴다 — from()에서는 채워지지 않고(리뷰
        // 자체엔 닉네임이 없음), 프로필의 후기 탭을 만드는 서비스 메서드가 조회한 뒤 withNicknames로
        // 채워 넣는다. 못 채운 곳(단건 조회 등)은 null이라 프론트가 이메일로 대체 표시한다.
        String reviewerNickname,
        String revieweeNickname,
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
                null,
                null,
                review.getRating(),
                review.getContent(),
                review.getImages().stream()
                        .sorted((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()))
                        .map(image -> image.getImageUrl())
                        .toList(),
                review.getCreatedAt()
        );
    }

    public ReviewResponseDto withNicknames(String reviewerNickname, String revieweeNickname) {
        return new ReviewResponseDto(id, postId, reviewerEmail, revieweeEmail, reviewerNickname, revieweeNickname,
                rating, content, imageUrls, createdAt);
    }
}
