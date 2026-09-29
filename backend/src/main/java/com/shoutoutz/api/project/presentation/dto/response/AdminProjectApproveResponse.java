package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import java.time.Instant;

public record AdminProjectApproveResponse(
        long projectId,
        ApprovalStatus approvalStatus,
        AdminProjectDecisionActor decidedBy,
        Instant decidedAt
) {
}
