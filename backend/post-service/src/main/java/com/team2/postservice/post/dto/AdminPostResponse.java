package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.PostStatus;

import java.time.LocalDateTime;

// 관리자 글 관리 목록용 — publiclyVisible 여부와 무관하게 전부 보여준다.
public record AdminPostResponse(
        Long id,
        String title,
        String authorEmail,
        String authorNickname,
        PostCategory category,
        PostStatus status,
        boolean publiclyVisible,
        LocalDateTime createdAt
) {
}
