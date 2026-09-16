package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.RestoredProject;
import java.time.Instant;

public record ProjectRestoreResponse(
        Long id,
        ApprovalStatus approvalStatus,
        Instant restoredAt
) {

    public static ProjectRestoreResponse from(RestoredProject restored) {
        return new ProjectRestoreResponse(
                restored.id(),
                restored.approvalStatus(),
                restored.restoredAt()
        );
    }
}
