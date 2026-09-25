package com.team2.postservice.ai.repository;

import com.team2.postservice.ai.entity.AiPostDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiPostDraftRepository extends JpaRepository<AiPostDraft, Long> {
    Optional<AiPostDraft> findByIdAndUserId(Long id, Long userId);

    // 수정 횟수 확인 + 차감을 하나의 UPDATE로 원자적으로 처리한다(조건이 안 맞으면 0행 갱신).
    // Gemini 호출처럼 느린 외부 요청 동안 DB 트랜잭션/행 잠금을 들고 있지 않기 위해, "자리 예약"과
    // "실제 AI 호출"을 분리했다 — 이 메서드가 자리 예약, 실패 시 revertRevision으로 환불한다.
    @Modifying
    @Query("update AiPostDraft d set d.revisionCount = d.revisionCount + 1 "
            + "where d.id = :id and d.userId = :userId and d.revisionCount < :max "
            + "and d.expiresAt > CURRENT_TIMESTAMP")
    int reserveRevision(@Param("id") Long id, @Param("userId") Long userId, @Param("max") int max);

    @Modifying
    @Query("update AiPostDraft d set d.revisionCount = d.revisionCount - 1 where d.id = :id and d.revisionCount > 0")
    int refundRevision(@Param("id") Long id);
}
