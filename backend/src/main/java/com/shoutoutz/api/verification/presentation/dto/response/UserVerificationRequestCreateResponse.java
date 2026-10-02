package com.shoutoutz.api.verification.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;

public record UserVerificationRequestCreateResponse(
        long requestId,
        UserType userType,
        String nickname,
        Integer cohort,
        String track,
        VerificationRequestStatus status,
        Instant requestedAt
) {

    public static UserVerificationRequestCreateResponse from(
            UserVerificationRequest request
    ) {
        return new UserVerificationRequestCreateResponse(
                request.getId(),
                request.getUserType(),
                request.getNickname(),
                request.getCohort(),
                request.getTrack(),
                request.getStatus(),
                request.getRequestedAt()
        );
    }
}
