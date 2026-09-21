package com.team2.paymentservice.payment.client;

import com.team2.common.payment.PaymentContext;
import com.team2.paymentservice.common.exception.CustomException;
import com.team2.paymentservice.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class PostServiceClient {
    private final RestClient postServiceRestClient;
    public PaymentContext getPaymentContext(Long postId) {
        try {
            PaymentContext context = postServiceRestClient.get().uri("/internal/payments/posts/{postId}", postId)
                    .retrieve().body(PaymentContext.class);
            if (context == null || !postId.equals(context.postId())) throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            return context;
        } catch (RestClientException failure) {
            throw new CustomException(ErrorCode.POST_SERVICE_UNAVAILABLE);
        }
    }
}
