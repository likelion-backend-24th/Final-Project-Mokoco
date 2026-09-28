package com.team2.chatservice.client.dto;

public record ChatMessageNotificationRequest(Long recipientId, Long postId, Long chatRoomId, String content) {}
