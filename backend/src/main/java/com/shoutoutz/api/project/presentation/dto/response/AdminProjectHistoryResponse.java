package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import java.time.Instant;
import java.util.List;

public record AdminProjectHistoryResponse(
        long projectId,
        List<Item> items
) {

    public AdminProjectHistoryResponse {
        items = List.copyOf(items);
    }

    public record Item(
            long historyId,
            ApprovalStatus fromStatus,
            ApprovalStatus toStatus,
            AdminProjectDecisionActor changedBy,
            String reason,
            Instant changedAt
    ) {
    }
}
