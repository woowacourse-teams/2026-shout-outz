package com.shoutoutz.api.verification.presentation.dto.request;

import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record AdminVerificationRequestFindAllRequest(
        @Pattern(
                regexp = "PENDING|APPROVED|REJECTED",
                message = "status는 PENDING, APPROVED, REJECTED 중 하나여야 합니다."
        )
        String status,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        String cursor
) {

    private static final int DEFAULT_SIZE = 20;

    public AdminVerificationRequestFindAllRequest {
        status = blankToNull(status);
        cursor = blankToNull(cursor);
    }

    public VerificationRequestStatus resolvedStatus() {
        return status == null ? VerificationRequestStatus.PENDING : VerificationRequestStatus.valueOf(status);
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
