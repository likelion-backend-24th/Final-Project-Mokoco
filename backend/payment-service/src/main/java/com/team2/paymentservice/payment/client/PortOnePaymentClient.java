package com.team2.paymentservice.payment.client;

import com.team2.paymentservice.common.exception.CustomException;
import com.team2.paymentservice.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class PortOnePaymentClient {

    private final RestClient portOneRestClient;

    public PortOnePaymentResponse getPayment(String paymentId) {
        try {
            return portOneRestClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .retrieve()
                    .body(PortOnePaymentResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.PORTONE_UNAVAILABLE);
        }
    }
}
