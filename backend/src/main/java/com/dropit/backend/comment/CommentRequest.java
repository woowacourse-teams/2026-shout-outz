package com.dropit.backend.comment;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 500, message = "댓글은 500자 이내여야 합니다.")
    String content,

    @Size(max = 60, message = "댓글 제목은 60자 이내여야 합니다.")
    String title,

    UUID parentId
) {
}
