package com.shoutoutz.api.bugreport.presentation.dto.response;

import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.domain.BugReportStatus;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import java.time.Instant;
import java.util.List;

public record BugReportAdminFindAllResponse(
        List<Item> items,
        SliceMetaResponse meta
) {

    public BugReportAdminFindAllResponse {
        items = List.copyOf(items);
    }

    public static BugReportAdminFindAllResponse from(
            List<BugReport> bugReports,
            SliceMetaResponse meta
    ) {
        return new BugReportAdminFindAllResponse(
                bugReports.stream().map(Item::from).toList(),
                meta
        );
    }

    public record Item(
            long bugReportId,
            String contentPreview,
            BugReportStatus status,
            Long reporterUserId,
            Instant createdAt,
            Instant updatedAt,
            Instant statusChangedAt,
            Long statusChangedByUserId
    ) {

        private static Item from(BugReport bugReport) {
            return new Item(
                    bugReport.getId(),
                    preview(bugReport.getContent()),
                    bugReport.getStatus(),
                    bugReport.getReporterUserId(),
                    bugReport.getCreatedAt(),
                    bugReport.getUpdatedAt(),
                    bugReport.getStatusChangedAt(),
                    bugReport.getStatusChangedByUserId()
            );
        }

        private static String preview(String content) {
            int codePointCount = content.codePointCount(0, content.length());
            if (codePointCount <= 200) {
                return content;
            }
            return content.substring(0, content.offsetByCodePoints(0, 200)) + "…";
        }
    }
}
