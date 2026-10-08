package com.shoutoutz.api.bugreport.presentation.dto.response;

import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import java.time.Instant;

public record BugReportCreateResponse(
        long bugReportId,
        BugReportStatus status,
        Instant createdAt
) {

    public static BugReportCreateResponse from(BugReport bugReport) {
        return new BugReportCreateResponse(
                bugReport.getId(),
                bugReport.getStatus(),
                bugReport.getCreatedAt()
        );
    }
}
