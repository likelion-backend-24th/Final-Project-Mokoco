package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PostRequestDto {
    public record Create(
            @NotBlank(message = "제목은 필수입니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            String content,

            @NotNull(message = "카테고리는 필수입니다.")
            PostCategory category
    ) {}

    public record Update(
            @NotBlank(message = "제목은 필수입니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            String content,

            @NotNull(message = "카테고리는 필수입니다.")
            PostCategory category
    ) {}
}