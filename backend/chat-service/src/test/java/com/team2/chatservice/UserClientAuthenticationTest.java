package com.team2.chatservice;

import com.sun.net.httpserver.HttpServer;
import com.team2.chatservice.client.UserClient;
import com.team2.chatservice.client.UserClientConfig;
import com.team2.chatservice.chatMessage.controller.ChatController;
import com.team2.chatservice.chatMessage.service.ChatService;
import com.team2.chatservice.config.TokenAuthenticationFilter;
import feign.Feign;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserClientAuthenticationTest {
    @Test void invalidJwtIs401ForControllerAndFilterWithoutLeakingFeignException() throws Exception {
        verifyFailure(401, "", 401);
    }

    @Test void wrongInternalKeyIsDependencyFailureNotInvalidUserToken() throws Exception {
        verifyFailure(401, "{\"code\":\"UNAUTHORIZED_INTERNAL_SERVICE\"}", 502);
    }

    @Test void userServerFailureIs502() throws Exception {
        verifyFailure(500, "", 502);
    }

    private void verifyFailure(int upstreamStatus, String errorBody, int expectedStatus) throws Exception {
        AtomicReference<String> sentToken = new AtomicReference<>();
        AtomicReference<String> sentKey = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/internal/users/verify-token", exchange -> {
            sentToken.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sentKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Key"));
            if (errorBody.contains("UNAUTHORIZED_INTERNAL_SERVICE"))
                exchange.getResponseHeaders().set("X-Internal-Auth-Error", "UNAUTHORIZED_INTERNAL_SERVICE");
            byte[] body = errorBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(upstreamStatus, body.length == 0 ? -1 : body.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            UserClientConfig config = new UserClientConfig();
            UserClient users = Feign.builder().contract(new SpringMvcContract())
                    .requestInterceptor(config.internalServiceAuthentication("test-only-key"))
                    .errorDecoder(config.userErrorDecoder())
                    .target(UserClient.class, "http://127.0.0.1:" + server.getAddress().getPort());
            ChatService chat = mock(ChatService.class);
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new ChatController(chat, users, mock(SimpMessagingTemplate.class))).build();
            mvc.perform(get("/api/chat-rooms/session").header("Authorization", "Bearer test-token"))
                    .andExpect(status().is(expectedStatus));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat-rooms/1/detail");
            request.addHeader("Authorization", "Bearer test-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            new TokenAuthenticationFilter(users).doFilter(request, response, (incoming, outgoing) -> {
                throw new AssertionError("Rejected authentication must not reach controller");
            });
            assertThat(response.getStatus()).isEqualTo(expectedStatus);
            assertThat(sentToken.get()).isEqualTo("test-token");
            assertThat(sentKey.get()).isEqualTo("test-only-key");
            verifyNoInteractions(chat);
        } finally { server.stop(0); }
    }
}
