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

    @Column(nullable = false)
    private Long repairerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FixDealStatus status = FixDealStatus.MATCHED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;


    public void changeStatus(FixDealStatus status) {
        this.status = status;

        if (status == FixDealStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

}
