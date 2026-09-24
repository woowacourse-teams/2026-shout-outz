package com.shoutoutz.api.feed.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;
import org.hibernate.validator.constraints.UniqueElements;

public record FeedSaveRequest(
        @NotBlank(message = "title은 필수입니다.")
        @CodePointLength(max = 100, message = "title은 100자를 초과할 수 없습니다.")
        String title,

        @NotBlank(message = "content는 필수입니다.")
        @CodePointLength(max = 5_000, message = "content는 5,000자를 초과할 수 없습니다.")
        String content,

        @NotEmpty(message = "categoryIds는 하나 이상 필요합니다.")
        @UniqueElements(message = "categoryIds에는 중복된 ID를 포함할 수 없습니다.")
        List<@NotNull Long> categoryIds,

        @NotNull(message = "mediaIds는 필수입니다.")
        @UniqueElements(message = "mediaIds에는 중복된 ID를 포함할 수 없습니다.")
        List<@NotNull Long> mediaIds
) {
}
