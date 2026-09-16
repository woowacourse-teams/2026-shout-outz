package com.shoutoutz.api.verification.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;

public record UserVerificationRequestResponse(
        Long requestId,
        UserType userType,
        String nickname,
        Integer cohort,
        String track,
        VerificationRequestStatus status,
        Instant requestedAt,
        Instant decidedAt,
        String reason
) {

    public static UserVerificationRequestResponse of(
            UserVerificationRequest request,
            Instant decidedAt,
            String reason
    ) {
        return new UserVerificationRequestResponse(
                request.getId(),
                request.getUserType(),
                request.getNickname(),
                request.getCohort(),
                request.getTrack(),
                request.getStatus(),
                request.getRequestedAt(),
                decidedAt,
                reason
        );
    }

    /**
     * 기능 도입 전에 이미 인증된 사용자는 신청 이력 없이 현재 프로필을 승인 상태로 보여준다.
     */
    public static UserVerificationRequestResponse legacyApproved(UserProfile profile) {
        Integer cohort = profile.getCohort() == null ? null : profile.getCohort().intValue();
        return new UserVerificationRequestResponse(
                null,
                profile.getUserType(),
                profile.getDisplayName().value(),
                cohort,
                profile.getTrack(),
                VerificationRequestStatus.APPROVED,
                null,
                null,
                null
        );
    }
}
