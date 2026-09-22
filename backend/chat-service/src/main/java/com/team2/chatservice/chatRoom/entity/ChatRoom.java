package com.team2.chatservice.chatRoom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FixDeal은 post-service가 소유한 엔티티라 여기서는 JPA 연관관계 없이 id만 들고 있는다
    // (같은 DB를 공유하지만 서비스 경계를 넘는 연관관계는 맺지 않는다).
    @Column(name = "fix_deal_id", unique = true)
    private Long fixDealId;

    // 채택 전부터 채팅을 열 수 있도록 방을 제안(Proposal) 기준으로 식별한다.
    @Column(name = "proposal_id", unique = true)
    private Long proposalId;

    private Long requesterId;
    private Long repairerId;
    private Long postId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean hasParticipant(Long userId) {
        return userId != null && (userId.equals(requesterId) || userId.equals(repairerId));
    }

    // 제안이 채택돼 FixDeal이 생기면, 이미 열려있던(채택 전) 채팅방에 그 거래를 이어붙인다.
    // 멱등: 이미 같은 거래에 연결돼 있으면 조용히 성공 처리(after-commit 비동기 재시도가 안전하도록).
    public void attachDeal(Long dealId, Long dealProposalId, Long dealRequesterId, Long dealRepairerId, Long dealPostId) {
        if (fixDealId != null && fixDealId.equals(dealId)) return;
        if (!Objects.equals(proposalId, dealProposalId) || !Objects.equals(requesterId, dealRequesterId)
                || !Objects.equals(repairerId, dealRepairerId) || !Objects.equals(postId, dealPostId))
            throw new IllegalArgumentException("Deal does not match chat participants and proposal");
        if (fixDealId != null)
            throw new IllegalStateException("Chat room already linked to a deal");
        this.fixDealId = dealId;
    }

    // 채택 취소로 거래가 무효화되면 채팅방과의 연결을 끊는다 — 방(대화 기록) 자체는 유지된다. 멱등.
    public void detachDeal(Long dealId) {
        if (fixDealId != null && fixDealId.equals(dealId)) {
            this.fixDealId = null;
        }
    }
}
