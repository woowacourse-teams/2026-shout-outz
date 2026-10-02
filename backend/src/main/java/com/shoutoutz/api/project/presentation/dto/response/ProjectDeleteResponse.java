package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectDeletion;
import java.time.Instant;

public record ProjectDeleteResponse(
        String slug,
        Instant deletedAt,
        Instant restoreDeadlineAt
) {

    public static ProjectDeleteResponse from(ProjectDeletion deletion) {
        return new ProjectDeleteResponse(
                deletion.getProjectSlug(),
                deletion.getDeletedAt(),
                deletion.getRestoreDeadlineAt()
        );
    }
}
