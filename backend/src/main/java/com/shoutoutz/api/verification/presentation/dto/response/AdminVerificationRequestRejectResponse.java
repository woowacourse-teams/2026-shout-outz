package com.shoutoutz.api.verification.presentation.dto.response;

import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;

public record AdminVerificationRequestRejectResponse(
        long requestId,
        VerificationRequestStatus status,
        String reason,
        AdminVerificationRequestDecisionActor decidedBy,
        Instant decidedAt
) {

    public static AdminVerificationRequestRejectResponse of(
            UserVerificationRequest request,
            String reason,
            AdminVerificationRequestDecisionActor decidedBy
    ) {
        return new AdminVerificationRequestRejectResponse(
                request.getId(),
                request.getStatus(),
                reason,
                decidedBy,
                request.getDecidedAt()
        );
    }
}
