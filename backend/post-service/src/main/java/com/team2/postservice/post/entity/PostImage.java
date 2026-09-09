package com.team2.postservice.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false, length = 255)
    private String storedFileName;

    @Column(nullable = false)
    private int sortOrder;

    @Builder
    public PostImage(Post post, String imageUrl, String storedFileName, int sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.storedFileName = storedFileName;
        this.sortOrder = sortOrder;
    }
}
