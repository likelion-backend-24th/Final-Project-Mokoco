package com.team2.postservice.ai.repository;

import com.team2.postservice.ai.entity.AiPostDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiPostDraftRepository extends JpaRepository<AiPostDraft, Long> {
    Optional<AiPostDraft> findByIdAndUserId(Long id, Long userId);

    // 수정 횟수 확인 + 차감을 하나의 UPDATE로 원자적으로 처리한다(조건이 안 맞으면 0행 갱신).
    // Gemini 호출처럼 느린 외부 요청 동안 DB 트랜잭션/행 잠금을 들고 있지 않기 위해, "자리 예약"과
    // "실제 AI 호출"을 분리했다 — 이 메서드가 자리 예약, 실패 시 revertRevision으로 환불한다.
    // Spring Data JPA 리포지토리는 기본으로 SimpleJpaRepository의 클래스 레벨
    // @Transactional(readOnly=true)를 물려받는다 — save()/delete() 같은 CRUD 메서드는
    // 자체적으로 쓰기 트랜잭션을 오버라이드해두지만, 커스텀 @Modifying 쿼리는 명시하지 않으면
    // 읽기 전용 트랜잭션 안에서 실행돼 MySQL이 UPDATE를 거부한다. 그래서 여기서 직접 override한다.
    //
    // 만료 비교는 SQL CURRENT_TIMESTAMP(DB 서버 시각) 대신 자바에서 만든 LocalDateTime.now()를
    // 파라미터로 받는다 — expiresAt도 애초에 같은 LocalDateTime.now() 기준으로 저장했는데,
    // 운영 MySQL 컨테이너는 TZ를 안 맞춰줘서 기본 UTC로 뜬다(post-service는 Asia/Seoul). DB
    // 서버 시각과 비교하면 9시간이 어긋나 만료된 초안이 안 만료된 것으로(혹은 반대로) 판정된다.
    @Transactional
    @Modifying
    @Query("update AiPostDraft d set d.revisionCount = d.revisionCount + 1 "
            + "where d.id = :id and d.userId = :userId and d.revisionCount < :max "
            + "and d.expiresAt > :now")
    int reserveRevision(@Param("id") Long id, @Param("userId") Long userId, @Param("max") int max, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("update AiPostDraft d set d.revisionCount = d.revisionCount - 1 where d.id = :id and d.revisionCount > 0")
    int refundRevision(@Param("id") Long id);
}
