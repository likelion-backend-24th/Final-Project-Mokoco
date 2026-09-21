package com.team2.postservice.post.entity;

import jakarta.persistence.*;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_posts_nearby", columnList = "regionCode,publiclyVisible,status,createdAt,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 100)
    private String regionName; // 지역 이름 필드

    @Column(length = 20)
    private String regionCode;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean publiclyVisible = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PostImage> images = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Post(String title, String content, Long authorId, String regionName, String regionCode, PostCategory category) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.regionName = regionName; // 빌더에 지역 이름 추가
        this.regionCode = regionCode;
        this.category = category;
        this.status = PostStatus.WAITING;
    }

    public void update(String title, String content, PostCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void updateStatusToMatched() {
        changeStatus(PostStatus.MATCHED);
    }

    public void changeStatus(PostStatus status) {
        if (status == null) throw new CustomException(ErrorCode.INVALID_INPUT);
        if (this.status == status) return;
        boolean allowed = this.status == PostStatus.WAITING && status == PostStatus.MATCHED
                || this.status == PostStatus.MATCHED && (status == PostStatus.WAITING || status == PostStatus.COMPLETED);
        if (!allowed) throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        this.status = status;
    }

    public void changeVisibility(boolean publiclyVisible) {
        this.publiclyVisible = publiclyVisible;
    }

    public boolean isAcceptingProposals() {
        return publiclyVisible && status == PostStatus.WAITING;
    }

    public PostImage addImage(String imageUrl, String storedFileName) {
        PostImage postImage = PostImage.builder()
                .post(this)
                .imageUrl(imageUrl)
                .storedFileName(storedFileName)
                .sortOrder(images.size())
                .build();
        images.add(postImage);
        return postImage;
    }

    public void removeImage(PostImage image) {
        images.remove(image);
    }
}
