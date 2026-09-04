package com.team2.postservice.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private Integer sortOrder;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public PostImage(Post post, String imageUrl, String storedFileName, Integer sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.storedFileName = storedFileName;
        this.sortOrder = sortOrder;
    }
}