package com.team2.postservice.client;

import com.team2.postservice.client.dto.AdminPaymentPageResponse;
import com.team2.postservice.client.dto.AdminPaymentSummaryResponse;
import com.team2.postservice.client.dto.PaymentClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "payment-service",
        url = "${services.payment-service.url:http://localhost:8083}",
        configuration = PaymentClientConfig.class
)
public interface PaymentClient {

    @GetMapping("/internal/payments/post/{postId}")
    PaymentClientResponse getPaymentByPostId(@PathVariable Long postId);

    @PostMapping("/internal/payments/post/{postId}/settle")
    void settle(@PathVariable Long postId);

    @GetMapping("/internal/payments/admin/summary")
    AdminPaymentSummaryResponse getAdminSummary();

    @GetMapping("/internal/payments/admin/list")
    AdminPaymentPageResponse getAdminPayments(
            @RequestParam("status") String status,
            @RequestParam("page") int page,
            @RequestParam("size") int size);
}
