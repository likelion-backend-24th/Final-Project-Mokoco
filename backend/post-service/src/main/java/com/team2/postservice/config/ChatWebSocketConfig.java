package com.team2.postservice.config;

import com.team2.postservice.chatMessage.ChatService;
import com.team2.postservice.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final UserClient users;
    private final ChatService chat;
    @Value("${chat.allowed-origins:http://localhost:3000}") private String[] origins;

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat").setAllowedOrigins(origins);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setPreservePublishOrder(true);
    }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16384).setSendBufferSizeLimit(65536).setSendTimeLimit(10000);
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (headers == null || headers.getCommand() == null) return message;
                var command = headers.getCommand();
                if (command == StompCommand.CONNECT) {
                    String auth = headers.getFirstNativeHeader("Authorization");
                    if (auth == null || !auth.startsWith("Bearer ")) throw new MessagingException("Authentication required");
                    var user = users.verifyToken(auth.substring(7));
                    headers.setUser(() -> user.id().toString());
                    headers.getSessionAttributes().put("accessToken", auth.substring(7));
                } else if (command == StompCommand.SEND || command == StompCommand.SUBSCRIBE) {
                    if (headers.getUser() == null) throw new MessagingException("Authentication required");
                    // Revalidate expiry/revocation on each operation, including long-lived connections.
                    users.verifyToken((String) headers.getSessionAttributes().get("accessToken"));
                    String prefix = command == StompCommand.SEND ? "/app/chat/" : "/topic/chat/";
                    String destination = headers.getDestination();
                    if (destination == null || !destination.matches(java.util.regex.Pattern.quote(prefix) + "[0-9]+"))
                        throw new MessagingException("Destination not allowed");
                    chat.authorize(Long.valueOf(destination.substring(prefix.length())), Long.valueOf(headers.getUser().getName()));
                }
                return message;
            }
        });
    }
}
