package com.shoutoutz.api.resourcereport.presentation.dto.response;

import com.shoutoutz.api.resourcereport.domain.ResourceReport;
import com.shoutoutz.api.resourcereport.domain.ResourceReportStatus;
import java.time.Instant;

/**
 * @param handledAt       마지막으로 상태를 바꾼 시각. 바꾼 적이 없으면 null
 * @param handledByUserId 마지막으로 상태를 바꾼 관리자 id. 바꾼 적이 없으면 null
 */
public record ResourceReportStatusUpdateResponse(
        long reportId,
        ResourceReportStatus status,
        Instant handledAt,
        Long handledByUserId
) {

    public static ResourceReportStatusUpdateResponse from(ResourceReport report) {
        return new ResourceReportStatusUpdateResponse(
                report.getId(),
                report.getStatus(),
                report.getHandledAt(),
                report.getHandledByUserId()
        );
    }
}
