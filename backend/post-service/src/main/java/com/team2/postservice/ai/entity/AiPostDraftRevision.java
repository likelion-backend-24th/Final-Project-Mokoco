package com.team2.postservice.ai.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_post_draft_revisions", uniqueConstraints =
        @UniqueConstraint(name = "uk_ai_post_draft_revision", columnNames = {"draft_id", "revision_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiPostDraftRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "draft_id", nullable = false)
    private Long draftId;
    @Column(nullable = false)
    private int revisionNumber;
    @Column(length = 800)
    private String selectedText;
    @Column(length = 500)
    private String prompt;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AiPostDraftRevision(Long draftId, int revisionNumber, String selectedText, String prompt,
                               String resultJson, LocalDateTime createdAt) {
        this.draftId = draftId;
        this.revisionNumber = revisionNumber;
        this.selectedText = selectedText;
        this.prompt = prompt;
        this.resultJson = resultJson;
        this.createdAt = createdAt;
    }
}
