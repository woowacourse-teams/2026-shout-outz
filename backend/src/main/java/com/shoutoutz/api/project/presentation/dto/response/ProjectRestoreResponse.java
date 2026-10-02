package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.RestoredProject;
import java.time.Instant;

public record ProjectRestoreResponse(
        String slug,
        ApprovalStatus approvalStatus,
        Instant restoredAt
) {

    public static ProjectRestoreResponse from(RestoredProject restored) {
        return new ProjectRestoreResponse(
                restored.slug(),
                restored.approvalStatus(),
                restored.restoredAt()
        );
    }
}
