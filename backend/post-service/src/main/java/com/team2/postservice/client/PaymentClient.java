package com.team2.postservice.client;

import com.team2.postservice.client.dto.PaymentClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "payment-service",
        url = "${services.payment-service.url:http://localhost:8083}",
        configuration = UserClientConfig.class
)
public interface PaymentClient {

    @GetMapping("/internal/payments/post/{postId}")
    PaymentClientResponse getPaymentByPostId(@PathVariable Long postId);
}
