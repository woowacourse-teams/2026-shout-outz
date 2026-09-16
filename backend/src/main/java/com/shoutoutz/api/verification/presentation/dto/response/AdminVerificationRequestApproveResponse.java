package com.shoutoutz.api.verification.presentation.dto.response;

import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;

public record AdminVerificationRequestApproveResponse(
        long requestId,
        VerificationRequestStatus status,
        AdminVerificationRequestDecisionActor decidedBy,
        Instant decidedAt
) {

    public static AdminVerificationRequestApproveResponse of(
            UserVerificationRequest request,
            AdminVerificationRequestDecisionActor decidedBy
    ) {
        return new AdminVerificationRequestApproveResponse(
                request.getId(),
                request.getStatus(),
                decidedBy,
                request.getDecidedAt()
        );
    }
}
