package com.shoutoutz.api.bugreport.presentation.dto.response;

import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import java.time.Instant;

public record BugReportStatusUpdateResponse(
        long bugReportId,
        BugReportStatus status,
        Instant statusChangedAt,
        Long statusChangedByUserId,
        Instant updatedAt
) {

    public static BugReportStatusUpdateResponse from(BugReport bugReport) {
        return new BugReportStatusUpdateResponse(
                bugReport.getId(),
                bugReport.getStatus(),
                bugReport.getStatusChangedAt(),
                bugReport.getStatusChangedByUserId(),
                bugReport.getUpdatedAt()
        );
    }
}
