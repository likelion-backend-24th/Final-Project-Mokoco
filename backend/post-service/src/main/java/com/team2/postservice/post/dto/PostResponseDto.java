package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
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
            PostCategory category,
            PostStatus status,
            String regionName,
            List<ImageInfo> images,
            String createdAt,
            String updatedAt
    ) {
        public static Detail from(Post post) {
            return new Detail(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getAuthorEmail(),
                    post.getCategory(),
                    post.getStatus(),
                    post.getRegionName(),
                    post.getImages().stream()
                            .map(ImageInfo::from)
                            .toList(),
                    post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    post.getUpdatedAt() != null ? post.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
            );
        }
    }

    // 프론트가 개별 이미지를 삭제(DELETE /posts/{id}/images/{imageId})하려면 id가 필요해서
    // 단순 URL 문자열 대신 {id, imageUrl} 객체로 내려준다.
    public record ImageInfo(Long id, String imageUrl) {
        public static ImageInfo from(PostImage image) {
            return new ImageInfo(image.getId(), image.getImageUrl());
        }
    }
}
