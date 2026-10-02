package com.shoutoutz.api.project.infrastructure.mapper;

import com.shoutoutz.api.project.domain.ProjectApprovalHistory;
import com.shoutoutz.api.project.infrastructure.ProjectApprovalHistoryEntity;

public final class ProjectApprovalHistoryMapper {

    private ProjectApprovalHistoryMapper() {
    }

    public static ProjectApprovalHistoryEntity toEntity(ProjectApprovalHistory history) {
        return ProjectApprovalHistoryEntity.builder()
                .id(history.getId())
                .projectId(history.getProjectId())
                .changedBy(history.getChangedBy())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .reason(history.getReason())
                .changedAt(history.getChangedAt())
                .build();
    }

    public static ProjectApprovalHistory toDomain(ProjectApprovalHistoryEntity entity) {
        return ProjectApprovalHistory.reconstitute(
                entity.getId(),
                entity.getProjectId(),
                entity.getChangedBy(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getReason(),
                entity.getChangedAt()
        );
    }
}
