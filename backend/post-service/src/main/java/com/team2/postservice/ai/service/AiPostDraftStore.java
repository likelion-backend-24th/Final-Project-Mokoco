package com.team2.postservice.ai.service;

import com.team2.postservice.ai.entity.AiPostDraft;
import com.team2.postservice.ai.entity.AiPostDraftRevision;
import com.team2.postservice.ai.repository.AiPostDraftRepository;
import com.team2.postservice.ai.repository.AiPostDraftRevisionRepository;
import com.team2.postservice.common.exception.AiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiPostDraftStore {
    private final AiPostDraftRepository drafts;
    private final AiPostDraftRevisionRepository revisions;

    @Transactional
    public Session create(Long userId, String originalInputJson, String resultJson) {
        LocalDateTime now = LocalDateTime.now();
        AiPostDraft draft = drafts.save(new AiPostDraft(userId, originalInputJson, resultJson, now));
        revisions.save(new AiPostDraftRevision(draft.getId(), 0, null, null, resultJson, now));
        return new Session(draft.getId(), 0, 3);
    }

    public Claimed claim(Long id, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();
        if (drafts.claim(id, userId, token, now, now.minusMinutes(2)) == 1) {
            AiPostDraft draft = drafts.findById(id).orElseThrow();
            return new Claimed(token, draft.getOriginalInputJson(), draft.getCurrentResultJson());
        }

        AiPostDraft draft = drafts.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
                "AI_DRAFT_NOT_FOUND", "AI 초안을 찾을 수 없습니다."));
        if (!draft.getUserId().equals(userId)) throw error(HttpStatus.FORBIDDEN,
                "AI_DRAFT_ACCESS_DENIED", "본인의 AI 초안만 수정할 수 있습니다.");
        if (!draft.getExpiresAt().isAfter(now)) throw error(HttpStatus.GONE,
                "AI_DRAFT_EXPIRED", "AI 초안이 만료되었습니다. 사진을 다시 분석해주세요.");
        if (draft.getRetryCount() >= 3) throw error(HttpStatus.CONFLICT,
                "AI_RETRY_LIMIT_EXCEEDED", "AI 내용 수정은 최대 3회까지 가능합니다.");
        throw error(HttpStatus.CONFLICT, "AI_DRAFT_IN_PROGRESS", "같은 초안을 수정하고 있습니다. 잠시 후 다시 시도해주세요.");
    }

    @Transactional
    public Session complete(Long id, Long userId, String token, String selectedText, String prompt, String resultJson) {
        AiPostDraft draft = drafts.findLockedById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
                "AI_DRAFT_NOT_FOUND", "AI 초안을 찾을 수 없습니다."));
        if (!draft.getUserId().equals(userId) || !token.equals(draft.getProcessingToken())) {
            throw error(HttpStatus.CONFLICT, "AI_DRAFT_CONCURRENTLY_UPDATED", "AI 초안이 변경되었습니다. 최신 결과에서 다시 선택해주세요.");
        }
        LocalDateTime now = LocalDateTime.now();
        int revision = draft.getRetryCount() + 1;
        draft.completeRevision(token, resultJson, now);
        revisions.save(new AiPostDraftRevision(id, revision, selectedText, prompt, resultJson, now));
        return new Session(id, revision, 3 - revision);
    }

    public void release(Long id, String token) {
        drafts.release(id, token);
    }

    private AiException error(HttpStatus status, String code, String message) {
        return new AiException(status, code, message);
    }

    public record Session(Long draftId, int retryCount, int remainingRetries) {}
    public record Claimed(String token, String originalInputJson, String currentResultJson) {}
}
