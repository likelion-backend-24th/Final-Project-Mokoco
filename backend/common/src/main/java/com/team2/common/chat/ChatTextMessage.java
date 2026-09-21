package com.team2.common.chat;

public record ChatTextMessage(Long id, Long chatRoomId, Long senderId, String content) {}
