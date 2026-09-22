package com.team2.postservice.fixDeal.repository;

import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FixDealRepository extends JpaRepository<FixDeal, Long> {

    @org.springframework.data.jpa.repository.Query("select d from FixDeal d where d.proposalId = :proposalId and d.status <> com.team2.postservice.fixDeal.entity.FixDealStatus.CANCELED")
    Optional<FixDeal> findByProposalId(@org.springframework.data.repository.query.Param("proposalId") Long proposalId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from FixDeal d where d.id = :id")
    Optional<FixDeal> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<FixDeal> findByPostIdAndStatus(
            Long fixRequestId,
            FixDealStatus status
    );

    @org.springframework.data.jpa.repository.Query("select d from FixDeal d where d.postId = :postId and d.status <> com.team2.postservice.fixDeal.entity.FixDealStatus.CANCELED")
    Optional<FixDeal> findByPostId(@org.springframework.data.repository.query.Param("postId") Long postId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from FixDeal d where d.proposalId = :proposalId and d.status <> com.team2.postservice.fixDeal.entity.FixDealStatus.CANCELED")
    Optional<FixDeal> lockByProposalId(@org.springframework.data.repository.query.Param("proposalId") Long proposalId);

    boolean existsByProposalId(Long proposalId);

    Page<FixDeal> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    Page<FixDeal> findByRepairerIdOrderByCreatedAtDesc(Long repairerId, Pageable pageable);
}
