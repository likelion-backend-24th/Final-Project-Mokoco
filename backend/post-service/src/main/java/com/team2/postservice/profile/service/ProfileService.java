package com.team2.postservice.profile.service;

import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.contract.ContractRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final FixDealRepository fixDealRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final ContractRepository contractRepository;
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

        // "계약서 보기" 버튼을 채팅만 하고 계약서는 한 번도 안 만든 거래에도 항상 보여주고 있었다
        // — 채팅방 id가 있는 것만으로 계약서가 있다고 볼 수 없어서, 실제로 계약서가 있는 채팅방
        // id만 한 번에 조회해 각 항목에 표시한다. 채팅방이 아직 없는 거래는 이 맵에 값이 null로
        // 들어있을 수 있는데, 걸러내지 않으면 Set.copyOf가 null 원소에서 NPE를 던진다.
        List<Long> chatRoomIds = chatRoomIdsByDealId.values().stream().filter(Objects::nonNull).toList();
        Set<Long> chatRoomIdsWithContract = chatRoomIds.isEmpty()
                ? Set.of()
                : Set.copyOf(contractRepository.findDistinctChatRoomIdByChatRoomIdIn(chatRoomIds));

        var items = page.getContent().stream()
                .map(deal -> toItem(deal, asRequester, chatRoomIdsByDealId.get(deal.getId()), chatRoomIdsWithContract))
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

    private TransactionHistoryItemResponse toItem(FixDeal deal, boolean asRequester, Long chatRoomId,
            Set<Long> chatRoomIdsWithContract) {
        String postTitle = postRepository.findById(deal.getPostId())
                .map(Post::getTitle)
                .orElse("(삭제된 게시글)");

        Long counterpartId = asRequester ? deal.getRepairerId() : deal.getRequesterId();
        UserClientResponse counterpart = resolveUser(counterpartId);
        String counterpartEmail = counterpart != null ? counterpart.email() : "(알 수 없음)";
        String counterpartNickname = counterpart != null ? counterpart.nickname() : null;

        ReviewResponseDto review = reviewRepository.findByPostId(deal.getPostId())
                .map(ReviewResponseDto::from)
                .orElse(null);

        return new TransactionHistoryItemResponse(
                deal.getId(),
                deal.getPostId(),
                postTitle,
                asRequester ? "REQUESTER" : "REPAIRER",
                counterpartEmail,
                counterpartNickname,
                deal.getStatus(),
                deal.getCreatedAt(),
                deal.getCompletedAt(),
                review,
                chatRoomId,
                chatRoomId != null && chatRoomIdsWithContract.contains(chatRoomId)
        );
    }

    private UserClientResponse resolveUser(Long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    public MyWrittenReviewsResponse getMyWrittenReviews(String reviewerEmail, Pageable pageable) {
        Page<com.team2.postservice.review.entity.Review> page =
                reviewRepository.findByReviewerEmailOrderByCreatedAtDesc(reviewerEmail, pageable);

        // 목록에 이메일 대신 후기 대상(수리자)의 닉네임을 보여주려고 채워 넣는다. 페이지 안에
        // 같은 대상이 여러 번 나올 수 있어 이메일별로 한 번만 조회하도록 캐시한다.
        Map<String, String> nicknameByEmail = new HashMap<>();
        List<ReviewResponseDto> reviews = page.getContent().stream()
                .map(ReviewResponseDto::from)
                .map(dto -> dto.withNicknames(null,
                        nicknameByEmail.computeIfAbsent(dto.revieweeEmail(), this::resolveNicknameByEmail)))
                .toList();

        return new MyWrittenReviewsResponse(page.getTotalElements(), reviews);
    }

    private String resolveNicknameByEmail(String email) {
        try {
            UserClientResponse user = userClient.getUserByEmail(email);
            return user != null ? user.nickname() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
