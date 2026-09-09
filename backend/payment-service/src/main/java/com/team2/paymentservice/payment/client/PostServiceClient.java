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
public class PostServiceClient {

    private final RestClient postServiceRestClient;

    public PostInfoResponse getPost(Long postId) {
        try {
            return postServiceRestClient.get()
                    .uri("/posts/{id}", postId)
                    .retrieve()
                    .body(PostInfoResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PAYMENT);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.POST_SERVICE_UNAVAILABLE);
        }
    }
}
