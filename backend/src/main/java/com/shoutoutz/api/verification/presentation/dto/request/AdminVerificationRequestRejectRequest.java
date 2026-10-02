package com.shoutoutz.api.verification.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record AdminVerificationRequestRejectRequest(
        @NotBlank(message = "reason은 필수입니다.")
        @CodePointLength(max = 100, message = "reason은 100자를 초과할 수 없습니다.")
        String reason
) {

    public AdminVerificationRequestRejectRequest {
        reason = reason == null ? null : reason.trim();
    }
}
