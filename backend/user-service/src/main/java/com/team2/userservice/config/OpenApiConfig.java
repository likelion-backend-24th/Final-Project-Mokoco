package com.team2.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    // 게이트웨이가 이 서비스의 /v3/api-docs를 /user-service/v3/api-docs로 프록시해서 가져오는데,
    // 서버 URL을 명시하지 않으면 그 경로를 기준으로 상대경로가 잡혀 "Try it out"이 엉뚱한 곳을 호출한다.
    // 컨트롤러 매핑 자체가 이미 /api로 시작하고 게이트웨이도 그대로 전달하므로 루트로 고정한다.
    @Bean
    public OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("user-service API").description("회원가입/로그인/내 정보/관리자").version("v1"))
                .servers(List.of(new Server().url("/")));
    }
}
