package com.team2.postservice.fixDeal.repository;

import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FixDealRepository extends JpaRepository<FixDeal, Long> {

    Optional<FixDeal> findByProposalId(Long proposalId);

    Optional<FixDeal> findByPostIdAndStatus(
            Long fixRequestId,
            FixDealStatus status
    );

    Optional<FixDeal> findByPostId(Long postId);

    boolean existsByProposalId(Long proposalId);

    // 제안 카드에 "채택 N건" 노출용 — 그 수리공이 완료까지 마친 거래 건수
    long countByRepairerIdAndStatus(Long repairerId, FixDealStatus status);
}
