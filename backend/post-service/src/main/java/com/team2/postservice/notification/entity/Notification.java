package com.team2.postservice.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;   // 알림 받는 사람

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    private Long postId;             // 관련 게시글 (없을 수도 있음)

    private Long proposalId;         // 제안 관련 알림이 아닐 수도 있으니 nullable

    private Long chatRoomId;         // 채팅 알림일 때 이동할 채팅방

    @Column(nullable = false, length = 255)
    private String message;

    @Column(nullable = false)
    private boolean isRead;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Notification(Long recipientId, NotificationType type, Long postId,
                        Long proposalId, Long chatRoomId, String message) {
        this.recipientId = recipientId;
        this.type = type;
        this.postId = postId;
        this.proposalId = proposalId;
        this.chatRoomId = chatRoomId;
        this.message = message;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}