package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.ContentFormat;
import com.team2.postservice.post.entity.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PostRequestDto {
    // contentFormat은 일부러 필수로 안 둔다 — 안 보내는 클라이언트는 PLAIN_TEXT로 간주(서비스에서 처리).
    public record Create(
            @NotBlank(message = "제목은 필수입니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            String content,

            @NotNull(message = "카테고리는 필수입니다.")
            PostCategory category,

            ContentFormat contentFormat
    ) {}

    public record Update(
            @NotBlank(message = "제목은 필수입니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            String content,

            @NotNull(message = "카테고리는 필수입니다.")
            PostCategory category,

            ContentFormat contentFormat
    ) {}
}