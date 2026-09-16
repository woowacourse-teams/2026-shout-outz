package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;

/**
 * 프로젝트 팀원 응답 객체
 * 상세 조회와 목록 조회 응답이 함께 쓴다.
 * 가입한 사용자는 avatarImageId, 가입하지 않은 이관 팀원은 githubAvatarUrl과 githubProfileUrl을 가진다.
 */
public record ProjectMemberProfileResponse(
        Long userId,
        String handle,
        String displayName,
        Integer cohort,
        String track,
        Long avatarImageId,
        String githubAvatarUrl,
        String githubProfileUrl
) {

    public static ProjectMemberProfileResponse from(ProjectMemberProfile member) {
        return new ProjectMemberProfileResponse(
                member.userId(),
                member.handle(),
                member.displayName(),
                cohortValue(member.cohort()),
                trackValue(member.track()),
                member.avatarImageId(),
                member.githubAvatarUrl(),
                member.githubProfileUrl()
        );
    }

    private static Integer cohortValue(Cohort cohort) {
        if (cohort == null) {
            return null;
        }
        return cohort.getValue();
    }

    private static String trackValue(Track track) {
        if (track == null) {
            return null;
        }
        return track.getValue();
    }
}
