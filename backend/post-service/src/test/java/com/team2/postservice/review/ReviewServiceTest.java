package com.team2.postservice.review;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.review.dto.ReviewRequestDto;
import com.team2.postservice.review.dto.UserReviewsResponseDto;
import com.team2.postservice.review.entity.Review;
import com.team2.postservice.review.repository.ReviewRepository;
import com.team2.postservice.post.service.FileStorageService;
import com.team2.postservice.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    final ReviewRepository reviews = mock(ReviewRepository.class);
    final FixDealRepository fixDeals = mock(FixDealRepository.class);
    final UserClient users = mock(UserClient.class);
    final FileStorageService fileStorageService = mock(FileStorageService.class);
    final ReviewService service = new ReviewService(reviews, fixDeals, users, fileStorageService);

    private FixDeal completedFixDeal(LocalDateTime completedAt) {
        return FixDeal.builder()
                .id(1L)
                .postId(10L)
                .proposalId(20L)
                .requesterId(100L)
                .repairerId(200L)
                .status(FixDealStatus.COMPLETED)
                .completedAt(completedAt)
                .build();
    }

    private ReviewRequestDto.Create validRequest() {
        return new ReviewRequestDto.Create(10L, 5, "친절하고 꼼꼼하게 고쳐주셨어요.");
    }

    @Test void requesterCreatesReviewOnCompletedDeal() {
        FixDeal deal = completedFixDeal(LocalDateTime.now().minusHours(1));
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));
        when(users.getUserById(200L))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));
        when(reviews.existsByPostId(10L)).thenReturn(false);
        when(reviews.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createReview(validRequest(), null, "requester@test.com");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getReviewerEmail()).isEqualTo("requester@test.com");
        assertThat(saved.getRevieweeEmail()).isEqualTo("repairer@test.com");
        assertThat(saved.getRating()).isEqualTo(5);
    }

    @Test void rejectsWhenFixDealNotFound() {
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReview(validRequest(), null, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FIX_DEAL_NOT_FOUND_FOR_REVIEW);
    }

    @Test void rejectsWhenReviewerIsNotRequester() {
        FixDeal deal = completedFixDeal(LocalDateTime.now().minusHours(1));
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("stranger@test.com"))
                .thenReturn(new UserClientResponse(999L, "stranger@test.com", "stranger", "region"));

        assertThatThrownBy(() -> service.createReview(validRequest(), null, "stranger@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_REVIEW_CREATE);

        verify(reviews, never()).save(any());
    }

    @Test void rejectsWhenTransactionNotCompleted() {
        FixDeal deal = FixDeal.builder()
                .id(1L).postId(10L).proposalId(20L)
                .requesterId(100L).repairerId(200L)
                .status(FixDealStatus.REPAIR_DONE)
                .build();
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));

        assertThatThrownBy(() -> service.createReview(validRequest(), null, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSACTION_NOT_COMPLETED);
    }

    @Test void rejectsWhenReviewDeadlineExpired() {
        FixDeal deal = completedFixDeal(LocalDateTime.now().minusDays(4));
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));

        assertThatThrownBy(() -> service.createReview(validRequest(), null, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_DEADLINE_EXPIRED);
    }

    @Test void allowsReviewJustBeforeDeadline() {
        // 완료 후 2일 23시간 — 아직 3일이 안 지남
        FixDeal deal = completedFixDeal(LocalDateTime.now().minusDays(2).minusHours(23));
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));
        when(users.getUserById(200L))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));
        when(reviews.existsByPostId(10L)).thenReturn(false);
        when(reviews.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createReview(validRequest(), null, "requester@test.com");

        verify(reviews).save(any(Review.class));
    }

    @Test void rejectsDuplicateReview() {
        FixDeal deal = completedFixDeal(LocalDateTime.now().minusHours(1));
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));
        when(reviews.existsByPostId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.createReview(validRequest(), null, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_REVIEW);

        verify(reviews, never()).save(any());
    }

    @Test void rejectsRatingOutOfRange() {
        ReviewRequestDto.Create invalidLow = new ReviewRequestDto.Create(10L, 0, "내용");
        ReviewRequestDto.Create invalidHigh = new ReviewRequestDto.Create(10L, 6, "내용");

        assertThatThrownBy(() -> service.createReview(invalidLow, null, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RATING);

        assertThatThrownBy(() -> service.createReview(invalidHigh, null, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RATING);

        verifyNoInteractions(fixDeals);
    }

    @Test void getUserReviewsReturnsAverageAndList() {
        Review review = Review.builder()
                .postId(10L).reviewerEmail("requester@test.com").revieweeEmail("repairer@test.com")
                .rating(5).content("좋아요").build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = new PageImpl<>(List.of(review), pageable, 1);

        when(reviews.findByRevieweeEmailOrderByCreatedAtDesc("repairer@test.com", pageable)).thenReturn(page);
        when(reviews.findAverageRatingByRevieweeEmail("repairer@test.com")).thenReturn(4.5);

        UserReviewsResponseDto result = service.getUserReviews("repairer@test.com", pageable);

        assertThat(result.averageRating()).isEqualTo(4.5);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.reviews()).hasSize(1);
        assertThat(result.reviews().get(0).revieweeEmail()).isEqualTo("repairer@test.com");
    }

    @Test void savesImagesInOrderWhenCreatingReview() {
        FixDeal deal = completedFixDeal(LocalDateTime.now().minusHours(1));
        when(fixDeals.findByPostId(10L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));
        when(users.getUserById(200L))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));
        when(reviews.existsByPostId(10L)).thenReturn(false);
        when(reviews.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile first = new MockMultipartFile("images", "a.jpg", "image/jpeg", "a".getBytes());
        MultipartFile second = new MockMultipartFile("images", "b.jpg", "image/jpeg", "b".getBytes());
        when(fileStorageService.store(first)).thenReturn(new FileStorageService.StoredFile("https://cdn/a.jpg", "a-stored.jpg"));
        when(fileStorageService.store(second)).thenReturn(new FileStorageService.StoredFile("https://cdn/b.jpg", "b-stored.jpg"));

        service.createReview(validRequest(), List.of(first, second), "requester@test.com");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(captor.capture());
        List<String> urls = captor.getValue().getImages().stream().map(image -> image.getImageUrl()).toList();
        assertThat(urls).containsExactly("https://cdn/a.jpg", "https://cdn/b.jpg");
    }

    @Test void rejectsWhenTooManyImages() {
        List<MultipartFile> sixImages = List.of(
                new MockMultipartFile("images", "1.jpg", "image/jpeg", "1".getBytes()),
                new MockMultipartFile("images", "2.jpg", "image/jpeg", "2".getBytes()),
                new MockMultipartFile("images", "3.jpg", "image/jpeg", "3".getBytes()),
                new MockMultipartFile("images", "4.jpg", "image/jpeg", "4".getBytes()),
                new MockMultipartFile("images", "5.jpg", "image/jpeg", "5".getBytes()),
                new MockMultipartFile("images", "6.jpg", "image/jpeg", "6".getBytes())
        );

        assertThatThrownBy(() -> service.createReview(validRequest(), sixImages, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REVIEW_IMAGES);

        verifyNoInteractions(fixDeals);
    }
}
