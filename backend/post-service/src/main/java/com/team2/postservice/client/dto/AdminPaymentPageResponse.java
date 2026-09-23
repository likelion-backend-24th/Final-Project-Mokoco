package com.team2.postservice.client.dto;

import java.util.List;

// payment-service가 내려주는 Spring Page의 JSON 중 필요한 필드만 매핑한다.
// Feign은 Page 인터페이스를 직접 역직렬화할 수 없어서, 같은 모양의 평범한 DTO로 받는다.
public record AdminPaymentPageResponse(
        List<AdminPaymentClientResponse> content,
        long totalElements,
        int totalPages,
        int number
) {
}
