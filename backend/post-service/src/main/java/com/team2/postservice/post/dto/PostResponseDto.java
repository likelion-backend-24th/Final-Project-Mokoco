package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostImage;
import com.team2.postservice.post.entity.PostStatus;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PostResponseDto {
    public record Detail(
            Long id,
            String title,
            String content,
            String authorEmail,
            PostStatus status,
            List<String> images, // 이미지 URL 리스트 필드 추가
            String createdAt,
            String updatedAt
    ) {
        public static Detail from(Post post) {
            return new Detail(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthorEmail(),
                    post.getStatus(),
                    post.getImages().stream()
                            .map(PostImage::getImageUrl)
                            .toList(),
                    post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    post.getUpdatedAt() != null ? post.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
            );
        }
    }
}
