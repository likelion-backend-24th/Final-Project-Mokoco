package com.team2.postservice.chatRoom.entity;

import com.team2.postservice.fixDeal.entity.FixDeal;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fix_deal_id", unique = true)
    private FixDeal fixDeal;

    // Nullable during migration of existing deal-based rooms.
    @Column(name = "proposal_id", unique = true)
    private Long proposalId;
    private Long requesterId;
    private Long repairerId;
    private Long postId;

    public Long getProposalId() { return proposalId != null ? proposalId : fixDeal == null ? null : fixDeal.getProposalId(); }
    public Long getRequesterId() { return requesterId != null ? requesterId : fixDeal == null ? null : fixDeal.getRequesterId(); }
    public Long getRepairerId() { return repairerId != null ? repairerId : fixDeal == null ? null : fixDeal.getRepairerId(); }
    public Long getPostId() { return postId != null ? postId : fixDeal == null ? null : fixDeal.getPostId(); }

    public boolean hasParticipant(Long userId) {
        return userId != null && (userId.equals(getRequesterId()) || userId.equals(getRepairerId()));
    }

    public void attachDeal(FixDeal deal) {
        if (!Objects.equals(getProposalId(), deal.getProposalId())
                || !Objects.equals(getRequesterId(), deal.getRequesterId())
                || !Objects.equals(getRepairerId(), deal.getRepairerId())
                || !Objects.equals(getPostId(), deal.getPostId()))
            throw new IllegalArgumentException("Deal does not match chat participants and proposal");
        if (fixDeal != null && !Objects.equals(fixDeal.getId(), deal.getId()))
            throw new IllegalStateException("Chat room already linked to a deal");
        this.fixDeal = deal;
    }

    public void detachDeal(FixDeal deal) {
        if (this.fixDeal == null) {
            return;
        }

        if (!java.util.Objects.equals(this.fixDeal.getId(), deal.getId())) {
            throw new IllegalArgumentException("Deal does not match linked deal");
        }

        this.fixDeal = null;
    }

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();


}
