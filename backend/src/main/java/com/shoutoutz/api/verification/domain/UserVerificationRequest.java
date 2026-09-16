package com.shoutoutz.api.verification.domain;

import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import lombok.Getter;

@Getter
public class UserVerificationRequest {

    private final Long id;
    private final Long userId;
    private final UserType userType;
    private final String nickname;
    private final Integer cohort;
    private final String track;
    private final VerificationRequestStatus status;
    private final Instant requestedAt;
    private final Instant decidedAt;

    private UserVerificationRequest(
            Long id,
            Long userId,
            UserType userType,
            String nickname,
            Integer cohort,
            String track,
            VerificationRequestStatus status,
            Instant requestedAt,
            Instant decidedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.userType = userType;
        this.nickname = nickname;
        this.cohort = cohort;
        this.track = track;
        this.status = status;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
    }

    public static UserVerificationRequest create(
            long userId,
            UserType userType,
            String nickname,
            Integer cohort,
            String track,
            Instant requestedAt
    ) {
        String normalizedNickname = nickname == null ? null : nickname.trim();
        String normalizedTrack = track == null ? null : track.trim();
        UserVerificationRequestValidator.validate(
                userId,
                userType,
                normalizedNickname,
                cohort,
                normalizedTrack,
                requestedAt
        );
        return new UserVerificationRequest(
                null,
                userId,
                userType,
                normalizedNickname,
                cohort,
                normalizedTrack,
                VerificationRequestStatus.PENDING,
                requestedAt,
                null
        );
    }

    public static UserVerificationRequest reconstitute(
            Long id,
            Long userId,
            UserType userType,
            String nickname,
            Integer cohort,
            String track,
            VerificationRequestStatus status,
            Instant requestedAt,
            Instant decidedAt
    ) {
        return new UserVerificationRequest(
                id,
                userId,
                userType,
                nickname,
                cohort,
                track,
                status,
                requestedAt,
                decidedAt
        );
    }

    public UserVerificationRequest approve(Instant decidedAt) {
        return reconstitute(
                id,
                userId,
                userType,
                nickname,
                cohort,
                track,
                VerificationRequestStatus.APPROVED,
                requestedAt,
                decidedAt
        );
    }
}
