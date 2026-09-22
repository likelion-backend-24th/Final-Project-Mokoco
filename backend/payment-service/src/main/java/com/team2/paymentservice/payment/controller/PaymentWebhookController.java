package com.team2.paymentservice.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.paymentservice.payment.service.PaymentService;
import com.team2.paymentservice.payment.webhook.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestBody String rawBody
    ) {
        boolean valid = signatureVerifier.verify(webhookId, webhookTimestamp, webhookSignature, rawBody);
        if (!valid) {
            // 서명이 안 맞으면 위조 요청일 수 있으니 401로 거절
            return ResponseEntity.status(401).build();
        }

        JsonNode root = parse(rawBody);
        if (root == null) {
            return ResponseEntity.ok().build();
        }

        String type = root.path("type").asText("");
        if (!type.startsWith("Transaction.")) {
            // 빌링키 발급 등 결제와 무관한 이벤트 무시
            return ResponseEntity.ok().build();
        }

        JsonNode data = root.path("data");
        String portonePaymentId = data.path("paymentId").asText(null);
        if (portonePaymentId == null) {
            return ResponseEntity.ok().build();
        }

        try {
            paymentService.handleWebhookPayment(portonePaymentId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    private JsonNode parse(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return null;
        }
    }
}
