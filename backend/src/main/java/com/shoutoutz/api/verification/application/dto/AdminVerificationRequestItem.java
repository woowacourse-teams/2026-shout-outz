package com.shoutoutz.api.verification.application.dto;

import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;

public record AdminVerificationRequestItem(
        long requestId,
        long userId,
        String handle,
        UserType userType,
        String nickname,
        Integer cohort,
        String track,
        VerificationRequestStatus status,
        Instant requestedAt
) {

    public AdminVerificationRequestCursor toCursor() {
        return new AdminVerificationRequestCursor(requestedAt, requestId);
    }
}
