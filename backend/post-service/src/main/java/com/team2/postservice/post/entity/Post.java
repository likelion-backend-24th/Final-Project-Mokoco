package com.team2.postservice.post.entity;

import jakarta.persistence.*;
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
@Table(name = "posts", indexes = @Index(name = "idx_posts_nearby", columnList = "regionCode,publiclyVisible,status,bumpedAt,id"))
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
    private String authorEmail;

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

    // 목록 정렬 기준 시각. 최초에는 createdAt과 같고, 끌어올리기(bump) 시 갱신된다.
    @Column(nullable = false)
    private LocalDateTime bumpedAt;

    // 제안은 있으나 채택하지 않았다는 리마인드 알림을 마지막으로 보낸 시각
    private LocalDateTime notAdoptedReminderSentAt;

    @Builder
    public Post(String title, String content, String authorEmail, String regionName, String regionCode, PostCategory category) {
        this.title = title;
        this.content = content;
        this.authorEmail = authorEmail;
        this.regionName = regionName; // 빌더에 지역 이름 추가
        this.regionCode = regionCode;
        this.category = category;
        this.status = PostStatus.WAITING;
        this.bumpedAt = LocalDateTime.now();
    }

    /** 제안이 오지 않아 게시글을 목록 맨 위로 끌어올린다. */
    public void bumpToTop() {
        this.bumpedAt = LocalDateTime.now();
    }

    /** 제안 미채택 리마인드 알림을 보냈음을 기록한다(중복 발송 방지용). */
    public void markNotAdoptedReminderSent() {
        this.notAdoptedReminderSentAt = LocalDateTime.now();
    }

    public void update(String title, String content, PostCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void updateStatusToMatched() {
        this.status = PostStatus.MATCHED;
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
