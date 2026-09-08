package com.team2.postservice.config;

import com.team2.postservice.chatMessage.ChatService;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import java.util.HashMap;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ChatWebSocketConfigTest {
    final UserClient users = mock(UserClient.class);
    final ChatService chat = mock(ChatService.class);

    ChannelInterceptor interceptor() {
        var registration = mock(ChannelRegistration.class);
        new ChatWebSocketConfig(users, chat).configureClientInboundChannel(registration);
        var capture = ArgumentCaptor.forClass(ChannelInterceptor.class);
        verify(registration).interceptors(capture.capture());
        return capture.getValue();
    }
    @Test void connectRequiresVerifiedToken() {
        var interceptor = interceptor();
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionAttributes(new HashMap<>());
        headers.setLeaveMutable(true);
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(message, null)).hasMessageContaining("Authentication required");
        headers.setNativeHeader("Authorization", "Bearer token");
        when(users.verifyToken("token")).thenReturn(new UserClientResponse(2L, "a@example.com", "a", "region"));
        interceptor.preSend(message, null);
        assertThat(headers.getUser().getName()).isEqualTo("2");
    }
    @Test void cannotPublishDirectlyToBrokerTopic() {
        var headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setUser(() -> "2");
        headers.setSessionAttributes(new HashMap<>());
        headers.setDestination("/topic/chat/1");
        headers.setLeaveMutable(true);
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
        assertThatThrownBy(() -> interceptor().preSend(message, null)).hasMessageContaining("Destination not allowed");
        verifyNoInteractions(chat);
    }
    @Test void subscriptionChecksRoomMembership() {
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setUser(() -> "2");
        headers.setSessionAttributes(new HashMap<>());
        headers.setDestination("/topic/chat/1");
        headers.setLeaveMutable(true);
        interceptor().preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
        verify(chat).authorize(1L, 2L);
    }
}
