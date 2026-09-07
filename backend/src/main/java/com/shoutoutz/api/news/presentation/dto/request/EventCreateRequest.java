package com.shoutoutz.api.news.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventCreateRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 100, message = "title은 100자를 초과할 수 없습니다.")
        String title,

        @NotBlank(message = "summary는 필수입니다.")
        @Size(max = 200, message = "summary는 200자를 초과할 수 없습니다.")
        String summary,

        @NotBlank(message = "body는 필수입니다.")
        @Size(max = 100_000, message = "body는 100,000자를 초과할 수 없습니다.")
        String body,

        @NotBlank(message = "authorName은 필수입니다.")
        @Size(max = 50, message = "authorName은 50자를 초과할 수 없습니다.")
        String authorName,

        @NotNull(message = "eventStartAt은 필수입니다.")
        Instant eventStartAt,

        @NotNull(message = "eventEndAt은 필수입니다.")
        Instant eventEndAt,

        @Valid
        Cta cta
) {

    public record Cta(
            @NotBlank(message = "cta.label은 필수입니다.")
            @Size(max = 100, message = "cta.label은 100자를 초과할 수 없습니다.")
            String label,

            @NotBlank(message = "cta.url은 필수입니다.")
            @Size(max = 2_048, message = "cta.url은 2,048자를 초과할 수 없습니다.")
            String url
    ) {
    }
}
