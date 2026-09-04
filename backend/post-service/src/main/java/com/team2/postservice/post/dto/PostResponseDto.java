package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostStatus;

import java.time.format.DateTimeFormatter;

public class PostResponseDto {
    public record Detail(
            Long id,
            String title,
            String content,
            String authorEmail,
            PostStatus status,
            String createdAt, // String 타입으로 변경
            String updatedAt  // String 타입으로 변경
    ) {
        public static Detail from(Post post) {
            return new Detail(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthorEmail(),
                    post.getStatus(),
                    post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    post.getUpdatedAt() != null ? post.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
            );
        }
    }
}