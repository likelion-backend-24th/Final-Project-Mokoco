package com.team2.paymentservice.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
        String id,
        String status,
        Amount amount,
        String customData
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(int total) {
    }
}
