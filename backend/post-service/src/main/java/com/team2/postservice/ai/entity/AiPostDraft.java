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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String originalInputJson;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String currentResultJson;
    @Column(nullable = false)
    private int retryCount;
    @Column(length = 36)
    private String processingToken;
    private LocalDateTime processingSince;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public AiPostDraft(Long userId, String originalInputJson, String currentResultJson, LocalDateTime now) {
        this.userId = userId;
        this.originalInputJson = originalInputJson;
        this.currentResultJson = currentResultJson;
        this.expiresAt = now.plusHours(24);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void completeRevision(String token, String resultJson, LocalDateTime now) {
        if (!token.equals(processingToken)) throw new IllegalStateException("AI draft processing token changed");
        currentResultJson = resultJson;
        retryCount++;
        processingToken = null;
        processingSince = null;
        updatedAt = now;
    }
}
