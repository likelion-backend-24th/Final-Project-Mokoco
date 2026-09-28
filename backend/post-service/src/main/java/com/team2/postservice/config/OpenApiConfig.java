package com.team2.postservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    // 컨트롤러 매핑 자체에는 /api 접두어가 없다(게이트웨이가 /api/posts/** 등을 /posts/**로 벗겨서 전달).
    // 예전엔 서버 URL을 /api로 뒀는데, Caddy가 실제 브라우저 트래픽을 위해 /api/posts/* 등을
    // 먼저 프론트(Next BFF)로 가로채버려서 "Try it out"이 405/404로 실패했다. Caddy에 새로 둔
    // /gateway-api/* 직결 경로(뒤에 /api/... 그대로 붙여 게이트웨이로 전달)로 맞춘다.
    @Bean
    public OpenAPI postServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("post-service API").description("글/제안/계약/채팅/후기/이력서").version("v1"))
                .servers(List.of(new Server().url("/gateway-api")));
    }
}
