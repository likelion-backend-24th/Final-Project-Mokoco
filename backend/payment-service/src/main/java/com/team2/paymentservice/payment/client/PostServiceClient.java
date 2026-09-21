package com.team2.paymentservice.payment.client;

import com.team2.common.payment.PaymentContext;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class PostServiceClient {

    private final RestClient postServiceRestClient;

    // 결제 준비/확정/웹훅 전부 이 값을 유일한 진실 소스로 삼는다 — 클라이언트가 보낸 금액·수신자는
    // 더 이상 믿지 않는다.
    public PaymentContext getPaymentContext(Long postId) {
        try {
            PaymentContext context = postServiceRestClient.get()
                    .uri("/api/internal/payments/context/{postId}", postId)
                    .retrieve()
                    .body(PaymentContext.class);
            if (context == null || !postId.equals(context.postId())) throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            return context;
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Conflict e) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.POST_SERVICE_UNAVAILABLE);
        }
    }
}
