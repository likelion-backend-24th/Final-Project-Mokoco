package com.team2.postservice.post.service;

import com.team2.postservice.notification.service.NotificationService;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 오랫동안 제안이 오지 않거나, 제안은 왔지만 채택하지 않은 게시글을 찾아
 * 작성자에게 알림을 보내고(필요 시) 목록 맨 위로 끌어올린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostReminderScheduler {

    private final PostRepository postRepository;
    private final ProposalRepository proposalRepository;
    private final NotificationService notificationService;

    @Value("${post.reminder.no-proposal-days:3}")
    private long noProposalDays;

    @Value("${post.reminder.not-adopted-days:3}")
    private long notAdoptedDays;

    @Scheduled(cron = "${post.reminder.cron:0 0 * * * *}") // 매 정시마다 실행
    @Transactional
    public void remindStalePosts() {
        LocalDateTime now = LocalDateTime.now();
        remindAndBumpPostsWithoutProposals(now);
        remindPostsWithUnadoptedProposals(now);
    }

    private void remindAndBumpPostsWithoutProposals(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(noProposalDays);
        List<Post> targets = postRepository.findWaitingPostsWithoutProposalsBumpedBefore(cutoff);

        for (Post post : targets) {
            try {
                notificationService.notifyNoProposal(post);
            } catch (Exception e) {
                log.warn("무제안 리마인드 알림 전송 실패 postId={}", post.getId(), e);
            }
            post.bumpToTop();
        }

        if (!targets.isEmpty()) {
            log.info("제안 없는 게시글 {}건 알림 발송 및 끌어올리기 완료", targets.size());
        }
    }

    private void remindPostsWithUnadoptedProposals(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(notAdoptedDays);
        List<Post> candidates = postRepository.findWaitingPostsWithUnadoptedProposals();

        int notified = 0;
        for (Post post : candidates) {
            LocalDateTime reference = referenceTimeFor(post);
            if (reference == null || reference.isAfter(cutoff)) continue;

            try {
                notificationService.notifyProposalNotAdopted(post);
            } catch (Exception e) {
                log.warn("미채택 리마인드 알림 전송 실패 postId={}", post.getId(), e);
            }
            post.markNotAdoptedReminderSent();
            notified++;
        }

        if (notified > 0) {
            log.info("제안 미채택 게시글 {}건 알림 발송 완료", notified);
        }
    }

    /** 마지막 리마인드 발송 시각이 있으면 그 시각을, 없으면 가장 먼저 도착한 제안 시각을 기준으로 삼는다. */
    private LocalDateTime referenceTimeFor(Post post) {
        if (post.getNotAdoptedReminderSentAt() != null) {
            return post.getNotAdoptedReminderSentAt();
        }
        List<Proposal> proposals = proposalRepository.findByPost(post);
        return proposals.stream()
                .map(Proposal::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }
}
