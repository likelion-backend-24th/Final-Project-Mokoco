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
    private Long fixRequestId;

    @Column(nullable = false)
    private Long fixProposalId;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long repairerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FixDealStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;


    public void changeStatus(FixDealStatus status) {
        this.status = status;

        if (status == FixDealStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

}
