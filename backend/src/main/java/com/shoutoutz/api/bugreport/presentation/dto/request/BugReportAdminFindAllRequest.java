package com.shoutoutz.api.bugreport.presentation.dto.request;

import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record BugReportAdminFindAllRequest(
        @Pattern(
                regexp = "OPEN|COMPLETED|ALL",
                message = "status는 OPEN, COMPLETED, ALL 중 하나여야 합니다."
        )
        String status,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        String cursor
) {

    private static final int DEFAULT_SIZE = 20;

    public BugReportAdminFindAllRequest {
        status = blankToNull(status);
        cursor = blankToNull(cursor);
    }

    public BugReportStatus resolvedStatus() {
        if (status == null) {
            return BugReportStatus.OPEN;
        }
        return "ALL".equals(status) ? null : BugReportStatus.valueOf(status);
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
