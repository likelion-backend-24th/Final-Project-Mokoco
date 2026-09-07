package com.team2.postservice.proposal.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProposalRequestDto {

    public record Create(
            @NotNull(message = "견적 금액은 필수입니다.")
            @Min(value = 0, message = "견적 금액은 0원 이상이어야 합니다.")
            Integer estimatedPrice,

            @NotBlank(message = "제안 내용은 필수입니다.")
            String content
    ) {}
}