package com.team2.postservice.ai.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// AI로 글 초안을 생성할 때마다 하나씩 만들어지는 세션. 이후 "선택한 문장만 AI로 다듬기"를
// 몇 번 썼는지(revisionCount, 최대 3회) 추적하고, 24시간 지나면 만료시켜 무한정 쌓이지 않게 한다.
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
}
