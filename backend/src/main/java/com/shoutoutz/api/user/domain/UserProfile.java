package com.shoutoutz.api.user.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserProfile {

    private final Long userId;
    private final String displayName;
    private final UserType userType;
    private final String track;
    private final Short cohort;
    private final String bio;
    private final String avatarUrl;
    private final String githubProfileUrl;
    private final String blogUrl;

    @Builder
    private UserProfile(
            Long userId,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String bio,
            String avatarUrl,
            String githubProfileUrl,
            String blogUrl
    ) {
        validate(userId, displayName, userType, track, cohort);
        this.userId = userId;
        this.displayName = displayName;
        this.userType = userType;
        this.track = track;
        this.cohort = cohort;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.githubProfileUrl = githubProfileUrl;
        this.blogUrl = blogUrl;
    }

    public static UserProfile initialize(Long userId, String displayName) {
        return new UserProfile(
                userId,
                displayName,
                UserType.GENERAL,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void validate(
            Long userId,
            String displayName,
            UserType userType,
            String track,
            Short cohort
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (displayName == null) {
            throw new IllegalArgumentException("사용자 표시 이름은 필수입니다.");
        }
        if (userType == null) {
            throw new IllegalArgumentException("사용자 유형은 필수입니다.");
        }
        if (userType == UserType.GENERAL && (track != null || cohort != null)) {
            throw new IllegalArgumentException("일반 사용자는 트랙과 기수를 가질 수 없습니다.");
        }
        if (userType == UserType.WOOWACOURSE_CREW && (track == null || cohort == null)) {
            throw new IllegalArgumentException("우아한테크코스 크루는 트랙과 기수가 필요합니다.");
        }
    }
}
