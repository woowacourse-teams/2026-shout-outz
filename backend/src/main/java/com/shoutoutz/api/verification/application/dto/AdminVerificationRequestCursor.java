package com.shoutoutz.api.verification.application.dto;

import java.time.Instant;
import java.util.Objects;

public record AdminVerificationRequestCursor(
        Instant requestedAt,
        long requestId
) {

    public AdminVerificationRequestCursor {
        Objects.requireNonNull(requestedAt, "커서의 신청 시각은 null일 수 없습니다.");
        if (requestId <= 0) {
            throw new IllegalArgumentException("커서의 인증 신청 ID는 0보다 커야 합니다.");
        }
    }
}
