package com.shoutoutz.api.bugreport.presentation.dto.response;

import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import java.time.Instant;

public record BugReportAdminDetailResponse(
        long bugReportId,
        String content,
        BugReportStatus status,
        Long reporterUserId,
        Instant createdAt,
        Instant updatedAt,
        Instant statusChangedAt,
        Long statusChangedByUserId
) {

    public static BugReportAdminDetailResponse from(BugReport bugReport) {
        return new BugReportAdminDetailResponse(
                bugReport.getId(),
                bugReport.getContent(),
                bugReport.getStatus(),
                bugReport.getReporterUserId(),
                bugReport.getCreatedAt(),
                bugReport.getUpdatedAt(),
                bugReport.getStatusChangedAt(),
                bugReport.getStatusChangedByUserId()
        );
    }
}
