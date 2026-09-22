package com.team2.postservice.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ContractDraftRequest(
        Long baseId,
        @NotNull Map<String,String> currentTerms,
        @NotNull @Size(max=2000) String instructions
) {}
