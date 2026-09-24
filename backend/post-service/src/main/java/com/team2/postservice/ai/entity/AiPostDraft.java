package com.team2.postservice.ai.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_post_drafts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiPostDraft {
    public static final int MAX_REVISIONS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int revisionCount;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AiPostDraft(Long userId) {
        this.userId = userId;
        this.revisionCount = 0;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusHours(24);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public int remainingRevisions() {
        return Math.max(MAX_REVISIONS - revisionCount, 0);
    }

    public void recordSuccessfulRevision() {
        if (revisionCount >= MAX_REVISIONS) {
            throw new IllegalStateException("AI revision limit exceeded");
        }
        revisionCount++;
    }
}
