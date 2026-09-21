package com.team2.postservice.notification.service;

import com.team2.postservice.client.UserClient;
import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.notification.dto.NotificationResponseDto;
import com.team2.postservice.notification.dto.NotificationSettingDto;
import com.team2.postservice.notification.entity.Notification;
import com.team2.postservice.notification.entity.NotificationSetting;
import com.team2.postservice.notification.entity.NotificationType;
import com.team2.postservice.notification.repository.NotificationRepository;
import com.team2.postservice.notification.repository.NotificationSettingRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.proposal.entity.Proposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.team2.postservice.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    /** STOMP 개인 알림 구독 목적지: 클라이언트는 /user/queue/notifications 를 구독한다. */
    public static final String USER_QUEUE = "/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;
    private final UserClient userClient;
    private final ChatClient chat;

    // ── 알림 생성 트리거 ────────────────────────────────────────────────

    /** 내 게시글에 새 수리 제안이 도착했을 때 (게시글 작성자에게) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyProposalReceived(Post post, Proposal proposal) {
        if (post.getAuthorId().equals(proposal.getRepairerId())) return;

        Long recipientId = post.getAuthorId();
        create(recipientId, NotificationType.PROPOSAL_RECEIVED,
                post.getId(), proposal.getId(), null,
                "\"" + post.getTitle() + "\" 게시글에 새로운 수리 제안이 도착했습니다.");
    }

    /** 내가 보낸 제안이 채택됐을 때 (수리공에게) */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyProposalAdopted(Post post, Proposal proposal) {
        if (post.getAuthorId().equals(proposal.getRepairerId())) return;

        Long recipientId = proposal.getRepairerId();
        create(recipientId, NotificationType.PROPOSAL_ADOPTED,
                post.getId(), proposal.getId(), null,
                "\"" + post.getTitle() + "\" 게시글에 보낸 수리 제안이 채택되었습니다.");
    }

    /**
     * 채팅방에 상대방이 새 메시지(답장)를 보냈을 때.
     * 호출부(트랜잭션 내)에서 수신자 id 등을 미리 계산해 넘긴다 — 별도 트랜잭션에서
     * 지연 로딩이 터지지 않도록 엔티티 대신 기본값만 받는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyChatMessage(Long recipientId, Long postId, Long chatRoomId, String content) {
        String preview = content.length() > 30 ? content.substring(0, 30) + "…" : content;

        create(recipientId, NotificationType.CHAT_MESSAGE,
                postId, null, chatRoomId, "새 채팅 메시지: " + preview);
    }

    private void create(Long recipientId, NotificationType type,
                        Long postId, Long proposalId, Long chatRoomId, String message) {
        if (!isEnabled(recipientId, type)) return;

        Notification saved = notificationRepository.save(Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .postId(postId)
                .proposalId(proposalId)
                .chatRoomId(chatRoomId)
                .message(message)
                .build());

        if (recipientId != null) {
            try {
                chat.notifyUser(recipientId, NotificationResponseDto.from(saved));
            } catch (Exception e) {
                log.warn("실시간 알림 전송 실패 recipientId={}", recipientId, e);
            }
        }
    }

    private boolean isEnabled(Long userId, NotificationType type) {
        return settingRepository.findByUserId(userId)
                .map(s -> s.isEnabled(type))
                .orElse(true);
    }

    // ── 조회 / 읽음 처리 ──────────────────────────────────────────────

    public List<NotificationResponseDto> getMyNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponseDto::from)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getRecipientId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_NOTIFICATION_ACCESS);
        }

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .forEach(Notification::markAsRead);
    }

    // ── 알림 수신 설정 ───────────────────────────────────────────────

    public NotificationSettingDto getSettings(Long userId) {
        return settingRepository.findByUserId(userId)
                .map(NotificationSettingDto::from)
                .orElseGet(NotificationSettingDto::defaults);
    }

    @Transactional
    public NotificationSettingDto updateSettings(Long userId, NotificationSettingDto request) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseGet(() -> new NotificationSetting(userId));
        setting.update(request.proposalReceived(), request.proposalAdopted(), request.chatMessage());
        return NotificationSettingDto.from(settingRepository.save(setting));
    }
}
