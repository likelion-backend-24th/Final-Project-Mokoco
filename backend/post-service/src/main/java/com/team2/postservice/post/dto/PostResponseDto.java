package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostStatus;

import java.time.LocalDateTime;

public class PostResponseDto {
    public record Detail(
            Long id,
            String title,
            String content,
            String authorEmail,
            PostStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Detail from(Post post) {
            return new Detail(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthorEmail(),
                    post.getStatus(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }
    }
}