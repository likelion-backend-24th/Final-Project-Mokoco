package com.team2.postservice.ai.repository;

import com.team2.postservice.ai.entity.AiPostDraft;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiPostDraftRepository extends JpaRepository<AiPostDraft, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select draft from AiPostDraft draft where draft.id = :id")
    Optional<AiPostDraft> findByIdForUpdate(@Param("id") Long id);
}
