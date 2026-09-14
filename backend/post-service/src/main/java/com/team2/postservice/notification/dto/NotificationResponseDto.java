package com.team2.postservice.notification.dto;

import com.team2.postservice.notification.entity.Notification;
import com.team2.postservice.notification.entity.NotificationType;

import java.time.format.DateTimeFormatter;

public record NotificationResponseDto(
        Long id,
        NotificationType type,
        Long postId,
        Long proposalId,
        Long chatRoomId,
        String message,
        boolean isRead,
        String createdAt
) {
    public static NotificationResponseDto from(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getType(),
                n.getPostId(),
                n.getProposalId(),
                n.getChatRoomId(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt() != null ? n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
        );
    }
}