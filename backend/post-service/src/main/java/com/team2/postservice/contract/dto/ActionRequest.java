package com.team2.postservice.contract.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActionRequest(
        @NotNull Long versionId,
        String documentHash,

        @Size(max = 80)
        String signerName,

        boolean consent
) {}
