package com.shoutoutz.api.verification.presentation.dto.response;

import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;
import java.util.List;

public record AdminVerificationRequestHistoryResponse(
        long requestId,
        List<Item> items
) {

    public AdminVerificationRequestHistoryResponse {
        items = List.copyOf(items);
    }

    public record Item(
            long historyId,
            VerificationRequestStatus fromStatus,
            VerificationRequestStatus toStatus,
            AdminVerificationRequestDecisionActor changedBy,
            String reason,
            Instant changedAt
    ) {
    }
}
