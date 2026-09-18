package com.team2.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    // 참고: payment-service의 /payments/** 경로는 게이트웨이 안에서만 쓰이고 외부(Caddy)로는
    // 열려 있지 않아서, 여기 문서의 "Try it out"은 게이트웨이에 모아둔 화면에서 실행되지 않는다.
    // 스키마·설명 열람용으로만 쓴다.
    @Bean
    public OpenAPI paymentServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("payment-service API").description("결제·정산 (PortOne 연동)").version("v1"))
                .servers(List.of(new Server().url("/")));
    }
}
