package com.shoutoutz.api.comment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.CodePointLength;

public record FeedCommentCreateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @CodePointLength(max = 500, message = "댓글 내용은 500자를 초과할 수 없습니다.")
        String content,

        @Positive(message = "parentId는 0보다 커야 합니다.")
        Long parentId
) {

    public FeedCommentCreateRequest {
        content = content == null ? null : content.strip();
    }
}
