package com.team2.postservice.profile.dto;

import java.util.List;

public record TransactionHistoryResponse(
        long totalCount,
        List<TransactionHistoryItemResponse> items
) {
}
