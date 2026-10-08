package com.shoutoutz.api.bugreport.infrastructure.mapper;

import com.shoutoutz.api.bugreport.domain.BugReport;
import com.shoutoutz.api.bugreport.infrastructure.BugReportEntity;

public final class BugReportMapper {

    private BugReportMapper() {
    }

    public static BugReportEntity toEntity(BugReport bugReport) {
        return BugReportEntity.builder()
                .id(bugReport.getId())
                .content(bugReport.getContent())
                .reporterUserId(bugReport.getReporterUserId())
                .status(bugReport.getStatus())
                .createdAt(bugReport.getCreatedAt())
                .updatedAt(bugReport.getUpdatedAt())
                .statusChangedAt(bugReport.getStatusChangedAt())
                .statusChangedByUserId(bugReport.getStatusChangedByUserId())
                .build();
    }

    public static BugReport toDomain(BugReportEntity entity) {
        return BugReport.reconstitute(
                entity.getId(),
                entity.getContent(),
                entity.getReporterUserId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStatusChangedAt(),
                entity.getStatusChangedByUserId()
        );
    }
}
