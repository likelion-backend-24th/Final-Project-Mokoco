package com.team2.postservice.review.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.service.FileStorageService;
import com.team2.postservice.review.dto.ReviewRequestDto;
import com.team2.postservice.review.dto.ReviewResponseDto;
import com.team2.postservice.review.dto.UserReviewsResponseDto;
import com.team2.postservice.review.entity.Review;
import com.team2.postservice.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final long REVIEW_DEADLINE_DAYS = 3;
    private static final int MAX_IMAGE_COUNT = 5;

    private final ReviewRepository reviewRepository;
    private final FixDealRepository fixDealRepository;
    private final UserClient userClient;
    private final FileStorageService fileStorageService;

    @Transactional
    public Long createReview(ReviewRequestDto.Create request, List<MultipartFile> images, String reviewerEmail) {
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new CustomException(ErrorCode.INVALID_RATING);
        }
        if (images != null && images.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.TOO_MANY_REVIEW_IMAGES);
        }

        FixDeal fixDeal = fixDealRepository.findByPostId(request.postId())
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND_FOR_REVIEW));

        UserClientResponse reviewer = userClient.getUserByEmail(reviewerEmail);
        if (!fixDeal.getRequesterId().equals(reviewer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_REVIEW_CREATE);
        }

        if (fixDeal.getStatus() != FixDealStatus.COMPLETED || fixDeal.getCompletedAt() == null) {
            throw new CustomException(ErrorCode.TRANSACTION_NOT_COMPLETED);
        }

        if (Duration.between(fixDeal.getCompletedAt(), LocalDateTime.now()).toDays() >= REVIEW_DEADLINE_DAYS) {
            throw new CustomException(ErrorCode.REVIEW_DEADLINE_EXPIRED);
        }

        if (reviewRepository.existsByPostId(request.postId())) {
            throw new CustomException(ErrorCode.DUPLICATE_REVIEW);
        }

        // FixDeal은 이메일이 아니라 userId(Long)로 당사자를 저장하므로,
        // 후기 대상(수리자)의 이메일은 user-service에 역조회해서 복원한다.
        UserClientResponse repairer = userClient.getUserById(fixDeal.getRepairerId());

        Review review = Review.builder()
                .postId(request.postId())
                .reviewerEmail(reviewerEmail)
                .revieweeEmail(repairer.email())
                .rating(request.rating())
                .content(request.content())
                .build();

        if (images != null) {
            int order = 0;
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;
                FileStorageService.StoredFile stored = fileStorageService.store(file);
                review.addImage(stored.imageUrl(), stored.storedFileName(), order++);
            }
        }

        reviewRepository.save(review);
        return review.getId();
    }

    public UserReviewsResponseDto getUserReviews(String revieweeEmail, Pageable pageable) {
        Page<Review> page = reviewRepository.findByRevieweeEmailOrderByCreatedAtDesc(revieweeEmail, pageable);
        Double average = reviewRepository.findAverageRatingByRevieweeEmail(revieweeEmail);

        return new UserReviewsResponseDto(
                average,
                page.getTotalElements(),
                page.getContent().stream().map(ReviewResponseDto::from).toList()
        );
    }

    public ReviewResponseDto getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        return ReviewResponseDto.from(review);
    }

    public ReviewResponseDto getReviewByPostId(Long postId) {
        Review review = reviewRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        return ReviewResponseDto.from(review);
    }

    public boolean existsByPostId(Long postId) {
        return reviewRepository.existsByPostId(postId);
    }
}
