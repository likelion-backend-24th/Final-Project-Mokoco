package com.team2.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    // payment-service의 /payments/** 경로는 프론트가 쓰는 공개 /api/payments/* 와 별개로
    // 게이트웨이 내부에서만 매치되던 경로라, 예전엔 "Try it out"이 실행될 방법이 없어 문서
    // 열람용으로만 뒀다. Caddy에 새로 둔 /gateway-api/* 직결 경로(뒤에 /payments/... 그대로
    // 붙여 게이트웨이로 전달)를 쓰면 게이트웨이의 payment-service 라우트에 그대로 매치된다.
    @Bean
    public OpenAPI paymentServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("payment-service API").description("결제·정산 (PortOne 연동)").version("v1"))
                .servers(List.of(new Server().url("/gateway-api")));
    }
}
