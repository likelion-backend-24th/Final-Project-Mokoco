package com.team2.postservice.proposal.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProposalRequestDto {

    public record Create(
            @NotNull(message = "견적 금액은 필수입니다.")
            @Min(value = 0, message = "견적 금액은 0원 이상이어야 합니다.")
            // Integer가 아니라 Long인 이유: 상한선보다 훨씬 큰 숫자(예: 30억)가 오면 Integer 파싱 자체가
            // 예외를 던져 @Max 검증까지 못 가고 일반 에러로 튕긴다. Long으로 받아 먼저 무사히 파싱한 뒤
            // 아래 @Max로 정상적인 검증 메시지를 띄운다.
            @Max(value = 999_999_999L, message = "견적 금액은 10억원 미만이어야 합니다.")
            Long estimatedPrice,

            @NotBlank(message = "제안 내용은 필수입니다.")
            String content,

            Boolean attachResume
    ) {}
}