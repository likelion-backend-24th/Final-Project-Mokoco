package com.team2.postservice.chatRoom.entity;

import com.team2.postservice.fixDeal.entity.FixDeal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    // 제안이 채택되면서(레거시 경로) 생성된 방은 이게 채워짐. 채택 전(제안 단계)에 만든 방은 null이어도 된다 —
    // postId/requesterId/repairerId를 직접 들고 있어서 FixDeal 없이도 동작한다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fix_deal_id", unique = true)
    private FixDeal fixDeal;

    // 채택 전부터 채팅을 열 수 있도록 방을 제안(Proposal) 기준으로 식별한다.
    @Column(name = "proposal_id", unique = true)
    private Long proposalId;

    private Long requesterId;
    private Long repairerId;
    private Long postId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 직접 들고 있는 필드를 우선 쓰고, 레거시(채택 시점에 생성된) 방은 fixDeal로 폴백한다.
    public Long getProposalId() {
        return proposalId != null ? proposalId : (fixDeal == null ? null : fixDeal.getProposalId());
    }

    public Long getRequesterId() {
        return requesterId != null ? requesterId : (fixDeal == null ? null : fixDeal.getRequesterId());
    }

    public Long getRepairerId() {
        return repairerId != null ? repairerId : (fixDeal == null ? null : fixDeal.getRepairerId());
    }

    public Long getPostId() {
        return postId != null ? postId : (fixDeal == null ? null : fixDeal.getPostId());
    }

    public boolean hasParticipant(Long userId) {
        return userId != null && (userId.equals(getRequesterId()) || userId.equals(getRepairerId()));
    }

    // 제안이 채택돼 FixDeal이 생기면, 이미 열려있던(채택 전) 채팅방에 그 거래를 이어붙인다.
    public void attachDeal(FixDeal deal) {
        if (!java.util.Objects.equals(getProposalId(), deal.getProposalId())
                || !java.util.Objects.equals(getRequesterId(), deal.getRequesterId())
                || !java.util.Objects.equals(getRepairerId(), deal.getRepairerId())
                || !java.util.Objects.equals(getPostId(), deal.getPostId()))
            throw new IllegalArgumentException("Deal does not match chat participants and proposal");
        if (fixDeal != null && !java.util.Objects.equals(fixDeal.getId(), deal.getId()))
            throw new IllegalStateException("Chat room already linked to a deal");
        this.fixDeal = deal;
    }
}
