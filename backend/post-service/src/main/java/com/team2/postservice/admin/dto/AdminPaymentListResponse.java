package com.team2.postservice.admin.dto;

import java.util.List;

// 프론트 관리자 테이블들이 공통으로 기대하는 페이지 모양(content/totalElements/totalPages/number).
public record AdminPaymentListResponse(
        List<AdminPaymentResponse> content,
        long totalElements,
        int totalPages,
        int number
) {
}
