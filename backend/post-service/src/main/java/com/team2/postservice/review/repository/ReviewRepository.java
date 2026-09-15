package com.team2.postservice.review.repository;

import com.team2.postservice.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByPostId(Long postId);

    Optional<Review> findByPostId(Long postId);

    Page<Review> findByRevieweeEmailOrderByCreatedAtDesc(String revieweeEmail, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.revieweeEmail = :revieweeEmail")
    Double findAverageRatingByRevieweeEmail(@Param("revieweeEmail") String revieweeEmail);
}
