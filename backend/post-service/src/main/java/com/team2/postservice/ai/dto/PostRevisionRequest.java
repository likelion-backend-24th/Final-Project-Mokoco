package com.team2.postservice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRevisionRequest(
        @NotNull Long draftId,
        @NotBlank @Size(max = 800) String selectedText,
        @Size(max = 500) String contextBefore,
        @Size(max = 500) String contextAfter,
        @NotBlank @Size(max = 500) String instruction
) {}
