package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.ContentFormat;
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
            ContentFormat contentFormat,
            String authorEmail,
            String authorNickname,
            PostCategory category,
            PostStatus status,
            String regionName,
            List<ImageInfo> images,
            String createdAt,
            String updatedAt,
            String regionCode,
            boolean publiclyVisible
    ) {
        public static Detail from(Post post, String authorNickname) {
            return new Detail(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getContentFormat(),
                    post.getAuthorEmail(),
                    authorNickname,
                    post.getCategory(),
                    post.getStatus(),
                    post.getRegionName(),
                    post.getImages().stream()
                            .map(ImageInfo::from)
                            .toList(),
                    post.getCreatedAt() != null
                            ? post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null,
                    post.getUpdatedAt() != null
                            ? post.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null,
                    post.getRegionCode(),
                    post.isPubliclyVisible()
            );
        }
    }

    // 프론트가 개별 이미지를 삭제하려면 image id가 필요하므로
    // URL 문자열이 아니라 {id, imageUrl} 형태로 내려준다.
    public record ImageInfo(
            Long id,
            String imageUrl
    ) {
        public static ImageInfo from(PostImage image) {
            return new ImageInfo(
                    image.getId(),
                    image.getImageUrl()
            );
        }
    }
}