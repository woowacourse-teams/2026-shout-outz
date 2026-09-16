package com.shoutoutz.api.verification.domain;

import java.time.Instant;
import lombok.Getter;

@Getter
public class UserVerificationRequestHistory {

    private final Long id;
    private final Long requestId;
    private final Long changedBy;
    private final VerificationRequestStatus fromStatus;
    private final VerificationRequestStatus toStatus;
    private final String reason;
    private final Instant changedAt;

    private UserVerificationRequestHistory(
            Long id,
            Long requestId,
            Long changedBy,
            VerificationRequestStatus fromStatus,
            VerificationRequestStatus toStatus,
            String reason,
            Instant changedAt
    ) {
        this.id = id;
        this.requestId = requestId;
        this.changedBy = changedBy;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public static UserVerificationRequestHistory initial(
            long requestId,
            Instant changedAt
    ) {
        return new UserVerificationRequestHistory(
                null,
                requestId,
                null,
                null,
                VerificationRequestStatus.PENDING,
                null,
                changedAt
        );
    }

    public static UserVerificationRequestHistory reconstitute(
            Long id,
            Long requestId,
            Long changedBy,
            VerificationRequestStatus fromStatus,
            VerificationRequestStatus toStatus,
            String reason,
            Instant changedAt
    ) {
        return new UserVerificationRequestHistory(
                id,
                requestId,
                changedBy,
                fromStatus,
                toStatus,
                reason,
                changedAt
        );
    }
}
