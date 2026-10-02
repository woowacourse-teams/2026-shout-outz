package com.shoutoutz.api.project.infrastructure.mapper;

import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.infrastructure.ProjectDeletionEntity;

public final class ProjectDeletionMapper {

    private ProjectDeletionMapper() {
    }

    public static ProjectDeletionEntity toEntity(ProjectDeletion deletion) {
        return ProjectDeletionEntity.builder()
                .id(deletion.getId())
                .projectId(deletion.getProjectId())
                .projectSlug(deletion.getProjectSlug())
                .projectTitle(deletion.getProjectTitle())
                .deletedBy(deletion.getDeletedBy())
                .deletionType(deletion.getDeletionType())
                .deletionReason(deletion.getDeletionReason())
                .deletedAt(deletion.getDeletedAt())
                .restoreDeadlineAt(deletion.getRestoreDeadlineAt())
                .restoredBy(deletion.getRestoredBy())
                .restoredAt(deletion.getRestoredAt())
                .build();
    }

    public static ProjectDeletion toDomain(ProjectDeletionEntity entity) {
        return ProjectDeletion.reconstitute(
                entity.getId(),
                entity.getProjectId(),
                entity.getProjectSlug(),
                entity.getProjectTitle(),
                entity.getDeletedBy(),
                entity.getDeletionType(),
                entity.getDeletionReason(),
                entity.getDeletedAt(),
                entity.getRestoreDeadlineAt(),
                entity.getRestoredBy(),
                entity.getRestoredAt()
        );
    }
}
