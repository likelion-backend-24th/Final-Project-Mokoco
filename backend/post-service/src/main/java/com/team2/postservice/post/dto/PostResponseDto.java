package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.PostImage;
import com.team2.postservice.post.entity.PostStatus;
import com.team2.postservice.post.entity.ContentFormat;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PostResponseDto {
    public record Detail(
            Long id,
            String title,
            String content,
            ContentFormat contentFormat,
            Long authorId,
            PostCategory category,
            PostStatus status,
            String regionName,
            List<String> images,
            String createdAt,
            String updatedAt,
            String regionCode,
            boolean publiclyVisible
    ) {
        public static Detail from(Post post) {
            return new Detail(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getContentFormat(),
                    post.getAuthorId(),
                    post.getCategory(),
                    post.getStatus(),
                    post.getRegionName(),
                    post.getImages().stream()
                            .map(PostImage::getImageUrl)
                            .toList(),
                    post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    post.getUpdatedAt() != null ? post.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    post.getRegionCode(), post.isPubliclyVisible()
            );
        }
    }
}
