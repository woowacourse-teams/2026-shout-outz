package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;

/**
 * 프로젝트 팀원 조회 모델
 * 가입한 사용자는 avatarImageId, 가입하지 않은 이관 팀원은 githubAvatarUrl과 githubProfileUrl을 가진다.
 */
public record ProjectMemberProfile(
        Long userId,
        String handle,
        String displayName,
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
        return new ProjectMemberProfile(userId, handle, displayName, cohort, track, avatarImageId, null, null);
    }

    /**
     * 탈퇴한 사용자는 사용자 프로필 조회와 같이 식별자만 남기고, 개인정보를 숨긴다.
     */
    public static ProjectMemberProfile withdrawn(long userId, String handle) {
        return new ProjectMemberProfile(userId, handle, WITHDRAWN_DISPLAY_NAME, null, null, null, null, null);
    }

    /**
     * 가입하지 않은 이관 팀원은 GitHub 정보로 보여주고, 기수는 프로젝트 기수로 채운다.
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
                Cohort.from(cohort),
                null,
                null,
                githubAvatarUrl,
                githubProfileUrl
        );
    }
}
