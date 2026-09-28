package com.team2.postservice.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractTerms(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String scope,
        @NotBlank @Size(max = 2000) String exclusions,
        @NotBlank @Size(max = 2000) String materials,
        // 동네수리는 원화만 다루고 소수점 단위가 없다 — Proposal.estimatedPrice/Payment 금액과 마찬가지로
        // 정수 원 단위만 허용한다(예전엔 fraction=2였는데, 프론트 number input의 소수점 step 때문에
        // 스크롤 휠 등으로 값이 0.01원 단위로 틀어지는 문제가 있었다).
        @NotNull @DecimalMin("1") @Digits(integer = 10, fraction = 0) BigDecimal totalAmount,
        @NotBlank @Size(max = 2000) String paymentTerms,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate startDate,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate endDate,
        @NotBlank @Size(max = 1000) String workLocation,
        @NotBlank @Size(max = 2000) String acceptanceCriteria,
        @NotBlank @Size(max = 2000) String warrantyTerms,
        @NotBlank @Size(max = 2000) String cancellationTerms,
        @NotBlank @Size(max = 2000) String additionalCostTerms
) {}
