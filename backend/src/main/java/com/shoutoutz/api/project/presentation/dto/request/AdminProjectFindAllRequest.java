package com.shoutoutz.api.project.presentation.dto.request;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record AdminProjectFindAllRequest(
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

    public AdminProjectFindAllRequest {
        status = blankToNull(status);
        cursor = blankToNull(cursor);
    }

    public ApprovalStatus resolvedStatus() {
        return status == null ? ApprovalStatus.PENDING : ApprovalStatus.valueOf(status);
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
