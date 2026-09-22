package com.team2.postservice.post.dto;

import com.team2.postservice.post.entity.ContentFormat;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.entity.ContentFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PostRequestDto {
    public record Create(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            @Size(max = 10000, message = "내용이 너무 깁니다.")
            String content,

            ContentFormat contentFormat,

            @NotNull(message = "카테고리는 필수입니다.")
            PostCategory category,

    ) {
        public Create { contentFormat = contentFormat == null ? ContentFormat.PLAIN_TEXT : contentFormat; }
    }

    public record Update(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            @Size(max = 10000, message = "내용이 너무 깁니다.")
            String content,

            ContentFormat contentFormat,

            @NotNull(message = "카테고리는 필수입니다.")
            PostCategory category,

            ContentFormat contentFormat
    ) {
        public Update { contentFormat = contentFormat == null ? ContentFormat.PLAIN_TEXT : contentFormat; }
    }
}
