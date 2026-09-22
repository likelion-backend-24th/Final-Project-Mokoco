package com.team2.postservice.profile.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.profile.dto.MyWrittenReviewsResponse;
import com.team2.postservice.profile.dto.TransactionHistoryItemResponse;
import com.team2.postservice.profile.dto.TransactionHistoryResponse;
import com.team2.postservice.review.dto.ReviewResponseDto;
import com.team2.postservice.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final FixDealRepository fixDealRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final UserClient userClient;

    public TransactionHistoryResponse getMyTransactions(Long userId, String role, Pageable pageable) {

        boolean asRequester = !"repairer".equalsIgnoreCase(role);
        Page<FixDeal> page = asRequester
                ? fixDealRepository.findByRequesterIdOrderByCreatedAtDesc(userId, pageable)
                : fixDealRepository.findByRepairerIdOrderByCreatedAtDesc(userId, pageable);

        java.util.List<TransactionHistoryItemResponse> items = page.getContent().stream()
                .map(deal -> toItem(deal, asRequester))
                .toList();

        return new TransactionHistoryResponse(page.getTotalElements(), items);
    }

    private TransactionHistoryItemResponse toItem(FixDeal deal, boolean asRequester) {
        String postTitle = postRepository.findById(deal.getPostId())
                .map(Post::getTitle)
                .orElse("(삭제된 게시글)");

        Long counterpartId = asRequester ? deal.getRepairerId() : deal.getRequesterId();
        String counterpartEmail = resolveEmail(counterpartId);

        ReviewResponseDto review = reviewRepository.findByPostId(deal.getPostId())
                .map(ReviewResponseDto::from)
                .orElse(null);

        return new TransactionHistoryItemResponse(
                deal.getId(),
                deal.getPostId(),
                postTitle,
                asRequester ? "REQUESTER" : "REPAIRER",
                counterpartEmail,
                deal.getStatus(),
                deal.getCreatedAt(),
                deal.getCompletedAt(),
                review
        );
    }

    private String resolveEmail(Long userId) {
        try {
            UserClientResponse user = userClient.getUserById(userId);
            return user != null ? user.email() : "(알 수 없음)";
        } catch (Exception e) {
            return "(알 수 없음)";
        }
    }

    public MyWrittenReviewsResponse getMyWrittenReviews(Long reviewerId, Pageable pageable) {
        Page<com.team2.postservice.review.entity.Review> page =
                reviewRepository.findByReviewerIdOrderByCreatedAtDesc(reviewerId, pageable);

        return new MyWrittenReviewsResponse(
                page.getTotalElements(),
                page.getContent().stream().map(ReviewResponseDto::from).toList()
        );
    }
}
