package com.shoutoutz.api.bugreport.domain;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import java.time.Instant;
import lombok.Getter;

@Getter
public class BugReport {

    private final Long id;
    private final String content;
    private final Long reporterUserId;
    private final BugReportStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant statusChangedAt;
    private final Long statusChangedByUserId;

    private BugReport(
            Long id,
            String content,
            Long reporterUserId,
            BugReportStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant statusChangedAt,
            Long statusChangedByUserId
    ) {
        this.id = id;
        this.content = content;
        this.reporterUserId = reporterUserId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statusChangedAt = statusChangedAt;
        this.statusChangedByUserId = statusChangedByUserId;
    }

    public static BugReport create(String content, Long reporterUserId, Instant createdAt) {
        String normalizedContent = content == null ? null : content.trim();
        if (normalizedContent == null || normalizedContent.isBlank()
                || normalizedContent.codePointCount(0, normalizedContent.length()) > 5_000) {
            throw new BadRequestException(BugReportErrorCode.BUG_REPORT_CONTENT_INVALID);
        }
        if (createdAt == null || (reporterUserId != null && reporterUserId < 1)) {
            throw new DomainValidationException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new BugReport(
                null,
                normalizedContent,
                reporterUserId,
                BugReportStatus.OPEN,
                createdAt,
                createdAt,
                null,
                null
        );
    }

    public static BugReport reconstitute(
            Long id,
            String content,
            Long reporterUserId,
            BugReportStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant statusChangedAt,
            Long statusChangedByUserId
    ) {
        return new BugReport(
                id,
                content,
                reporterUserId,
                status,
                createdAt,
                updatedAt,
                statusChangedAt,
                statusChangedByUserId
        );
    }

    public BugReport changeStatus(BugReportStatus newStatus, long adminUserId, Instant changedAt) {
        if (newStatus == null || adminUserId < 1 || changedAt == null) {
            throw new BadRequestException(BugReportErrorCode.BUG_REPORT_STATUS_INVALID);
        }
        if (status == newStatus) {
            return this;
        }
        return new BugReport(
                id,
                content,
                reporterUserId,
                newStatus,
                createdAt,
                changedAt,
                changedAt,
                adminUserId
        );
    }
}
