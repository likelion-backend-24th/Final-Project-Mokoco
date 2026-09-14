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
    @Column(unique = true)
    private Long proposalId;

    private Long postId;
    private Long requesterId;
    private Long repairerId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // fixDeal 관계 대신 직접 들고 있는 필드를 우선 쓰고, 레거시(채택 시점에 생성된) 방은 fixDeal로 폴백한다.
    public Long getPostId() {
        return postId != null ? postId : (fixDeal != null ? fixDeal.getPostId() : null);
    }

    public Long getRequesterId() {
        return requesterId != null ? requesterId : (fixDeal != null ? fixDeal.getRequesterId() : null);
    }

    public Long getRepairerId() {
        return repairerId != null ? repairerId : (fixDeal != null ? fixDeal.getRepairerId() : null);
    }
}
