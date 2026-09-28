package com.team2.chatservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    // 컨트롤러 매핑 자체가 이미 /api/chat-rooms로 시작하고 게이트웨이도 그대로 전달한다.
    // 서버 URL을 안 정해주면 springdoc이 문서를 가져온 경로(게이트웨이가 모아둔
    // /chat-service/v3/api-docs) 기준으로 상대경로를 잡아 "Try it out"이 엉뚱한 곳을 호출한다.
    // Caddy에 둔 /gateway-api/* 직결 경로(뒤에 컨트롤러 경로 그대로 붙여 게이트웨이로 전달)로 맞춘다.
    @Bean
    public OpenAPI chatServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("chat-service API").description("채팅방/메시지/첨부").version("v1"))
                .servers(List.of(new Server().url("/gateway-api")));
    }
}
