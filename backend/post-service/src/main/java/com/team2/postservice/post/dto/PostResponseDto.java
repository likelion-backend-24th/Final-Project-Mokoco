package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostImage;
import com.team2.postservice.post.entity.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponseDto {
    public record Detail(
            Long id,
            String title,
            String content,
            String authorEmail,
            PostStatus status,
            List<String> imageUrls,
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
                    post.getImages().stream()
                            .map(PostImage::getImageUrl)
                            .toList(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }
    }
}
