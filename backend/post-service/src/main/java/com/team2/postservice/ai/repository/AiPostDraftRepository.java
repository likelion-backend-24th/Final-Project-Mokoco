package com.team2.postservice.ai.repository;

import com.team2.postservice.ai.entity.AiPostDraft;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiPostDraftRepository extends JpaRepository<AiPostDraft, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update AiPostDraft d set d.processingToken = :token, d.processingSince = :now
            where d.id = :id and d.userId = :userId and d.retryCount < 3 and d.expiresAt > :now
              and (d.processingToken is null or d.processingSince < :staleBefore)
            """)
    int claim(@Param("id") Long id, @Param("userId") Long userId, @Param("token") String token,
              @Param("now") LocalDateTime now, @Param("staleBefore") LocalDateTime staleBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update AiPostDraft d set d.processingToken = null, d.processingSince = null where d.id = :id and d.processingToken = :token")
    int release(@Param("id") Long id, @Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from AiPostDraft d where d.id = :id")
    Optional<AiPostDraft> findLockedById(@Param("id") Long id);
}
