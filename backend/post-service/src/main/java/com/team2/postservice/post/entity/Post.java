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
@Table(name = "posts")
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
    public Post(String title, String content, String authorEmail) {
        this.title = title;
        this.content = content;
        this.authorEmail = authorEmail;
        this.status = PostStatus.WAITING;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updateStatusToMatched() {
        this.status = PostStatus.MATCHED;
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