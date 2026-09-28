package com.team2.postservice.fixDeal.dto;

public record ChatMessageNotificationRequest(Long recipientId, Long postId, Long chatRoomId, String content) {}
