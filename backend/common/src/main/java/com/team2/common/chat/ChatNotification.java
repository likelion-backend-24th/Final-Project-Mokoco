package com.team2.common.chat;

public record ChatNotification(Long recipientId, Long postId, Long chatRoomId, String content) {}
