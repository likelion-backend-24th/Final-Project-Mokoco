package com.team2.postservice.fixDeal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fix_deals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FixDeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long proposalId;

    @Column(nullable = false)
    private Long requesterId;

    @Column
    private String requesterEmail;

    @Column(nullable = false)
    private Long repairerId;

    @Column
    private String repairerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FixDealStatus status = FixDealStatus.MATCHED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;


    public void changeStatus(FixDealStatus status) {
        // 같은 상태로의 중복 호출을 안전한 no-op으로 만든다 — 호출부 락에 갭이 생기더라도
        // completedAt이 재호출마다 덮어써지는 것 같은 부작용을 막는 최후의 방어선.
        if (this.status == status) return;
        this.status = status;

        if (status == FixDealStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

}
