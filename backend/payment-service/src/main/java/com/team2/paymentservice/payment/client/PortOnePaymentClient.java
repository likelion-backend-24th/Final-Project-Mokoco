package com.team2.paymentservice.payment.client;

import com.team2.paymentservice.common.exception.CustomException;
import com.team2.paymentservice.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOnePaymentClient {

    private static final int MAX_ATTEMPTS = 4;
    private static final long RETRY_DELAY_MS = 700;

    private final RestClient portOneRestClient;

    // 이 API Secret이 여러 팀(스토어)에 걸쳐있는 공용 계정이라, storeId를 명시하지 않으면
    // 시크릿의 기본 스토어로 조회되어 우리 팀 결제 건이 404로 나온다.
    @Value("${portone.store-id}")
    private String storeId;

    public PortOnePaymentResponse getPayment(String paymentId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return portOneRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/payments/{paymentId}")
                                .queryParam("storeId", storeId)
                                .build(paymentId))
                        .retrieve()
                        .body(PortOnePaymentResponse.class);
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("PortOne 결제 조회 404 (paymentId={}, storeId={}, 시도 {}/{})",
                        paymentId, storeId, attempt, MAX_ATTEMPTS);
                if (attempt == MAX_ATTEMPTS) {
                    throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                }
                sleep(RETRY_DELAY_MS * attempt);
            } catch (RestClientException e) {
                throw new CustomException(ErrorCode.PORTONE_UNAVAILABLE);
            }
        }
        throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}