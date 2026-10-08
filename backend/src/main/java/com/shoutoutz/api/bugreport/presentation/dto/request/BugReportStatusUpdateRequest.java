package com.shoutoutz.api.bugreport.presentation.dto.request;

import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import jakarta.validation.constraints.NotNull;

public record BugReportStatusUpdateRequest(
        @NotNull(message = "status는 필수입니다.")
        BugReportStatus status
) {
}
