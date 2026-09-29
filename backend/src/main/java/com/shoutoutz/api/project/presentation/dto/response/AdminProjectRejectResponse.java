package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import java.time.Instant;

public record AdminProjectRejectResponse(
        long projectId,
        ApprovalStatus approvalStatus,
        String reason,
        AdminProjectDecisionActor decidedBy,
        Instant decidedAt
) {
}
