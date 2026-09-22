package com.team2.postservice.ai.dto;

import jakarta.validation.constraints.*;

public record PostDraftRevisionRequest(
        @NotNull @PositiveOrZero Integer selectionStart,
        @NotNull @Positive Integer selectionEnd,
        @NotBlank @Size(max = 800) String selectedText,
        @NotBlank @Size(max = 500) String prompt
) {}
