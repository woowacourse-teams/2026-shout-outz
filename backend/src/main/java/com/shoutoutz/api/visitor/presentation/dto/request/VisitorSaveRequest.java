package com.shoutoutz.api.visitor.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VisitorSaveRequest(
        @NotBlank(message = "example은 필수입니다.")
        String example
) {
}
