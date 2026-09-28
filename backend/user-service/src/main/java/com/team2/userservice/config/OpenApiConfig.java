package com.team2.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    // 컨트롤러 매핑 자체가 이미 /api로 시작하고 게이트웨이도 그대로 전달한다. 예전엔 서버 URL을
    // 루트(/)로 뒀는데, Caddy가 실제 브라우저 트래픽을 위해 /api/users/* 등을 먼저 프론트(BFF)로
    // 가로채버려서 "Try it out"이 엉뚱한 곳을 호출했다. Caddy에 새로 둔 /gateway-api/* 직결
    // 경로(뒤에 컨트롤러 경로 그대로 붙여 게이트웨이로 전달)로 맞춘다.
    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("user-service API").description("회원가입/로그인/내 정보/관리자").version("v1"))
                .servers(List.of(new Server().url("/gateway-api")));
    }
}
