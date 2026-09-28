package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.PostImage;

import java.time.LocalDateTime;

public record RecentCompletedPostResponse(
        Long id,
        String title,
        PostCategory category,
        String regionName,
        String thumbnailUrl,
        LocalDateTime completedAt
) {

    public static RecentCompletedPostResponse from(Post post) {
        String thumbnailUrl = post.getImages().stream()
                .findFirst()
                .map(PostImage::getImageUrl)
                .orElse(null);

        return new RecentCompletedPostResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory(),
                post.getRegionName(),
                thumbnailUrl,
                post.getUpdatedAt()
        );
    }
}
