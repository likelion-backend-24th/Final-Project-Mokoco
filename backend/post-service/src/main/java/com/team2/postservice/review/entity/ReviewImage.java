package com.team2.postservice.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ReviewImage(Review review, String imageUrl, String storedFileName, Integer sortOrder) {
        this.review = review;
        this.imageUrl = imageUrl;
        this.storedFileName = storedFileName;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }
}
