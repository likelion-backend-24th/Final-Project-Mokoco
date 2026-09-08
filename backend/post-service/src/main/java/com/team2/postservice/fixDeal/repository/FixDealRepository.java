package com.team2.postservice.fixDeal.repository;

import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FixDealRepository extends JpaRepository<FixDeal, Long> {

    Optional<FixDeal> findByFixProposalId(Long fixProposalId);

    Optional<FixDeal> findByFixRequestIdAndStatus(
            Long fixRequestId,
            FixDealStatus status
    );

    boolean existsByFixProposalId(Long fixProposalId);
}