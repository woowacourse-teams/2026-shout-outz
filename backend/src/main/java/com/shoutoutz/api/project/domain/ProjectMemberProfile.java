package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;

/**
 * 프로젝트 팀원 조회 모델
 * 가입한 사용자는 avatarImageId, 가입하지 않은 이관 팀원은 githubAvatarUrl과 githubProfileUrl을 가진다.
 */
public record ProjectMemberProfile(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        Cohort cohort,
        Track track,
        Long avatarImageId,
        String githubAvatarUrl,
        String githubProfileUrl
) {

    private static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자";

    public static ProjectMemberProfile user(
            long userId,
            String handle,
            String displayName,
            Cohort cohort,
            Track track,
            Long avatarImageId
    ) {
        return user(userId, handle, displayName, UserType.WOOWACOURSE_CREW, cohort, track, avatarImageId);
    }

    public static ProjectMemberProfile user(
            long userId,
            String handle,
            String displayName,
            UserType userType,
            Cohort cohort,
            Track track,
            Long avatarImageId
    ) {
        return new ProjectMemberProfile(
                userId,
                handle,
                displayName,
                userType,
                cohort,
                track,
                avatarImageId,
                null,
                null
        );
    }

    /**
     * 탈퇴한 사용자는 사용자 프로필 조회와 같이 식별자만 남기고, 개인정보를 숨긴다.
     */
    public static ProjectMemberProfile withdrawn(long userId, String handle) {
        return new ProjectMemberProfile(
                userId,
                handle,
                WITHDRAWN_DISPLAY_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * 가입하지 않은 이관 팀원은 GitHub 정보로 보여준다.
     * 프로젝트 기수는 이관 데이터 매칭을 위한 내부 정보로만 보관하며, 사용자 정보 응답에서는 노출하지 않는다.
     */
    public static ProjectMemberProfile archived(
            String displayName,
            int cohort,
            String githubAvatarUrl,
            String githubProfileUrl
    ) {
        return new ProjectMemberProfile(
                null,
                null,
                displayName,
                null,
                Cohort.from(cohort),
                null,
                null,
                githubAvatarUrl,
                githubProfileUrl
        );
    }
}
