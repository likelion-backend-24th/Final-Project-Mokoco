package com.team2.postservice.fixDeal.repository;

import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface FixDealRepository extends JpaRepository<FixDeal, Long> {

    // ChatRoom이 chat-service로 옮겨가면서, 계약 액션(초안/서명/진행)을 직렬화하던 잠금 대상이
    // ChatRoom에서 FixDeal로 바뀌었다 — ContractService.participant() 참고.
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select deal from FixDeal deal where deal.id = :id")
    Optional<FixDeal> lockById(@Param("id") Long id);

    Optional<FixDeal> findByProposalId(Long proposalId);

    // ProposalService.adoptProposal/cancelProposal이 이 락 없이 findByProposalId로 FixDeal을 읽고
    // 바로 mutate하면, 동시에 ContractService.advance()(참가자 락 보유)가 같은 FixDeal을 진행시키는
    // 경우와 TOCTOU 레이스가 난다 — 채택취소가 이미 REPAIRING으로 넘어간 거래를 되돌려버릴 수 있음.
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select deal from FixDeal deal where deal.proposalId = :proposalId")
    Optional<FixDeal> lockByProposalId(@Param("proposalId") Long proposalId);

    Optional<FixDeal> findByPostIdAndStatus(
            Long fixRequestId,
            FixDealStatus status
    );

    Optional<FixDeal> findByPostId(Long postId);

    boolean existsByProposalId(Long proposalId);

    // 제안 카드에 "채택 N건" 노출용 — 그 수리공이 완료까지 마친 거래 건수
    long countByRepairerIdAndStatus(Long repairerId, FixDealStatus status);

    Page<FixDeal> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    Page<FixDeal> findByRepairerIdOrderByCreatedAtDesc(Long repairerId, Pageable pageable);

    // 관리자 거래 현황판용
    Page<FixDeal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FixDeal> findByStatusOrderByCreatedAtDesc(FixDealStatus status, Pageable pageable);

    Page<FixDeal> findByStatusInOrderByCreatedAtDesc(Collection<FixDealStatus> statuses, Pageable pageable);
}
