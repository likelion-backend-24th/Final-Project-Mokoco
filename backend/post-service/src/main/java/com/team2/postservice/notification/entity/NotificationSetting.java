package com.team2.postservice.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 알림 수신 on/off 설정. 행이 없으면 모든 알림을 받는 것으로 간주한다.
 */
@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private boolean proposalReceived = true;

    @Column(nullable = false)
    private boolean proposalAdopted = true;

    @Column(nullable = false)
    private boolean chatMessage = true;

    public NotificationSetting(Long userId) {
        this.userId = userId;
        this.proposalReceived = true;
        this.proposalAdopted = true;
        this.chatMessage = true;
    }

    public void update(boolean proposalReceived, boolean proposalAdopted, boolean chatMessage) {
        this.proposalReceived = proposalReceived;
        this.proposalAdopted = proposalAdopted;
        this.chatMessage = chatMessage;
    }

    public boolean isEnabled(NotificationType type) {
        return switch (type) {
            case PROPOSAL_RECEIVED -> proposalReceived;
            case PROPOSAL_ADOPTED -> proposalAdopted;
            case CHAT_MESSAGE -> chatMessage;
        };
    }
}
