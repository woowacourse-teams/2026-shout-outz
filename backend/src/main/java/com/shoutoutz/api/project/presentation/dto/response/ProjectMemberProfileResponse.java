package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
import java.net.URI;
import java.util.Map;

/**
 * 프로젝트 팀원 응답 객체
 * 상세 조회와 목록 조회 응답이 함께 쓴다.
 * 가입한 사용자는 avatarUrl, 가입하지 않은 이관 팀원은 githubAvatarUrl과 githubProfileUrl을 가진다.
 */
public record ProjectMemberProfileResponse(
        String handle,
        String displayName,
        Integer cohort,
        String track,
        String avatarUrl,
        String githubAvatarUrl,
        String githubProfileUrl
) {

    public static ProjectMemberProfileResponse from(
            ProjectMemberProfile member,
            Map<Long, URI> mediaUrls
    ) {
        return new ProjectMemberProfileResponse(
                member.handle(),
                member.displayName(),
                cohortValue(member.cohort()),
                trackValue(member.track()),
                toUrl(mediaUrls, member.avatarImageId()),
                member.githubAvatarUrl(),
                member.githubProfileUrl()
        );
    }

    private static String toUrl(Map<Long, URI> mediaUrls, Long mediaId) {
        if (mediaId == null || mediaUrls == null) {
            return null;
        }
        URI url = mediaUrls.get(mediaId);
        return url == null ? null : url.toString();
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
