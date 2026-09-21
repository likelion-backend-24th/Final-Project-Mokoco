package com.team2.chatservice.chatRoom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_fix_deal",
                        columnNames = "fix_deal_id"
                ),
                @UniqueConstraint(name = "uk_chat_room_proposal", columnNames = "proposal_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // post-service의 FixDeal ID
    @Column(name = "fix_deal_id")
    private Long fixDealId;

    @Column(name = "proposal_id")
    private Long proposalId;

    private Long postId;
    private String postTitle;

    // user-service의 User ID
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    // user-service의 User ID
    @Column(name = "repairer_id", nullable = false)
    private Long repairerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    public static ChatRoom create(
            Long fixDealId,
            Long requesterId,
            Long repairerId
    ) {
        validateParticipants(requesterId, repairerId);

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.fixDealId = fixDealId;
        chatRoom.requesterId = requesterId;
        chatRoom.repairerId = repairerId;
        chatRoom.status = ChatRoomStatus.ACTIVE;
        chatRoom.createdAt = LocalDateTime.now();

        return chatRoom;
    }

    private static void validateParticipants(
            Long requesterId,
            Long repairerId
    ) {
        if (requesterId == null || repairerId == null) {
            throw new IllegalArgumentException("채팅 참여자 ID는 필수입니다.");
        }

        if (requesterId.equals(repairerId)) {
            throw new IllegalArgumentException(
                    "의뢰자와 수리자는 동일할 수 없습니다."
            );
        }
    }

    public boolean isParticipant(Long userId) {
        return requesterId.equals(userId)
                || repairerId.equals(userId);
    }

    public void updateContext(Long proposalId, Long postId, String postTitle, Long fixDealId) {
        if ((this.proposalId != null && !this.proposalId.equals(proposalId))
                || (this.postId != null && !this.postId.equals(postId)))
            throw new com.team2.common.exception.CustomException(com.team2.chatservice.common.exception.ErrorCode.INVALID_INPUT);
        this.proposalId = proposalId;
        this.postId = postId;
        this.postTitle = postTitle;
        this.fixDealId = fixDealId;
    }

    public void close() {
        if (this.status == ChatRoomStatus.CLOSED) return;
        this.status = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}
