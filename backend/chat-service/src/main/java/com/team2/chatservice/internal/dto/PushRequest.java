package com.team2.chatservice.internal.dto;

public record PushRequest(Long recipientId, Object payload) {}
