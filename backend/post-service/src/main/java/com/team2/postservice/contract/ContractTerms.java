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
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal totalAmount,
        @NotBlank @Size(max = 2000) String paymentTerms,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate startDate,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate endDate,
        @NotBlank @Size(max = 1000) String workLocation,
        @NotBlank @Size(max = 2000) String acceptanceCriteria,
        @NotBlank @Size(max = 2000) String warrantyTerms,
        @NotBlank @Size(max = 2000) String cancellationTerms,
        @NotBlank @Size(max = 2000) String additionalCostTerms
) {}
