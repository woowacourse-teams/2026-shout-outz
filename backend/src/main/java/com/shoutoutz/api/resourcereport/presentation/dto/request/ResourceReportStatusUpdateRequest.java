package com.shoutoutz.api.resourcereport.presentation.dto.request;

import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import jakarta.validation.constraints.NotNull;

public record ResourceReportStatusUpdateRequest(
        @NotNull(message = "status는 필수입니다.")
        ResourceReportStatus status
) {
}
