package com.team2.postservice.contract.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DraftRequest(
        Long baseId,
        @NotNull
        @Valid
        ContractTerms terms
) {

}
