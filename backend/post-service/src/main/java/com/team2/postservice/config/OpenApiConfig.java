package com.team2.postservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // 컨트롤러 매핑 자체에는 /api 접두어가 없다(게이트웨이가 /api/posts/** 등을 /posts/**로 벗겨서 전달).
    // 게이트웨이에 모인 Swagger UI에서 "Try it out"이 실제로 동작하려면, 문서상의 서버 기준 경로를
    // 게이트웨이가 보는 외부 경로(/api)로 맞춰줘야 한다.
    @Bean
    public OpenAPI postServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("post-service API").description("글/제안/계약/채팅/후기/이력서").version("v1"))
                .servers(java.util.List.of(new Server().url("/api")));
    }
}
