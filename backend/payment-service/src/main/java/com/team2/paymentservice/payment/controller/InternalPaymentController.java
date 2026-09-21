package com.team2.paymentservice.payment.controller;

import com.team2.paymentservice.payment.dto.PaymentResponseDto;
import com.team2.paymentservice.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/payments")
public class InternalPaymentController {
    private final PaymentService service;
    private final byte[] key;
    public InternalPaymentController(PaymentService service, @Value("${internal.service-key}") String key) {
        this.service = service; this.key = key.getBytes(StandardCharsets.UTF_8);
    }
    @GetMapping("/post/{postId}")
    public PaymentResponseDto get(@PathVariable Long postId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String supplied) {
        if (supplied == null || !MessageDigest.isEqual(key, supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return service.internalPayment(postId);
    }
}
