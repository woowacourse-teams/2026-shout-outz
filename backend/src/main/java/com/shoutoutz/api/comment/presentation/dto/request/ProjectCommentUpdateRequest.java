package com.shoutoutz.api.comment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record ProjectCommentUpdateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @CodePointLength(max = 500, message = "댓글 내용은 500자를 초과할 수 없습니다.")
        String content
) {

    public ProjectCommentUpdateRequest {
        content = content == null ? null : content.strip();
    }
}
