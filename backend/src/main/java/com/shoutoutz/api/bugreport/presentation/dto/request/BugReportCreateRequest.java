package com.shoutoutz.api.bugreport.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record BugReportCreateRequest(
        @NotBlank(message = "content는 필수입니다.")
        @CodePointLength(max = 5_000, message = "content는 5000자를 초과할 수 없습니다.")
        String content
) {

    public BugReportCreateRequest {
        content = content == null ? null : content.trim();
    }
}
