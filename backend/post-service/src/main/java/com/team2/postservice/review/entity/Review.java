package com.team2.postservice.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long postId;

    @Column(nullable = false)
    private Long reviewerId;

    @Column(nullable = false)
    private Long revieweeId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 1000)
    private String content;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewImage> images = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Review(Long postId, Long reviewerId, Long revieweeId, Integer rating, String content) {
        this.postId = postId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.rating = rating;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public ReviewImage addImage(String imageUrl, String storedFileName, int sortOrder) {
        ReviewImage image = ReviewImage.builder()
                .review(this)
                .imageUrl(imageUrl)
                .storedFileName(storedFileName)
                .sortOrder(sortOrder)
                .build();
        images.add(image);
        return image;
    }
}
