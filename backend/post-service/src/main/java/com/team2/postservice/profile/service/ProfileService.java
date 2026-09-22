package com.team2.postservice.profile.service;

import com.team2.postservice.client.ChatRoomClient;
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

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final FixDealRepository fixDealRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final UserClient userClient;
    private final ChatRoomClient chatRoomClient;

    public TransactionHistoryResponse getMyTransactions(String email, String role, Pageable pageable) {
        Long userId = userClient.getUserByEmail(email).id();

        boolean asRequester = !"repairer".equalsIgnoreCase(role);
        Page<FixDeal> page = asRequester
                ? fixDealRepository.findByRequesterIdOrderByCreatedAtDesc(userId, pageable)
                : fixDealRepository.findByRepairerIdOrderByCreatedAtDesc(userId, pageable);

        // 프로필의 거래 내역에서 바로 계약서(진행 상태·서명·정산)로 이동할 수 있도록 채팅방 id도 같이
        // 내려준다 — 건당 한 번씩 chat-service를 부르지 않도록 페이지 안의 거래 id를 한 번에 묶어 조회한다.
        List<Long> dealIds = page.getContent().stream().map(FixDeal::getId).toList();
        Map<Long, Long> chatRoomIdsByDealId = dealIds.isEmpty() ? Map.of() : chatRoomsByDealIds(dealIds);

        var items = page.getContent().stream()
                .map(deal -> toItem(deal, asRequester, chatRoomIdsByDealId.get(deal.getId())))
                .toList();

        return new TransactionHistoryResponse(page.getTotalElements(), items);
    }

    private Map<Long, Long> chatRoomsByDealIds(List<Long> dealIds) {
        try {
            return chatRoomClient.byFixDealIds(new ChatRoomClient.FixDealIdsRequest(dealIds));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private TransactionHistoryItemResponse toItem(FixDeal deal, boolean asRequester, Long chatRoomId) {
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
                review,
                chatRoomId
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

    public MyWrittenReviewsResponse getMyWrittenReviews(String reviewerEmail, Pageable pageable) {
        Page<com.team2.postservice.review.entity.Review> page =
                reviewRepository.findByReviewerEmailOrderByCreatedAtDesc(reviewerEmail, pageable);

        return new MyWrittenReviewsResponse(
                page.getTotalElements(),
                page.getContent().stream().map(ReviewResponseDto::from).toList()
        );
    }
}
