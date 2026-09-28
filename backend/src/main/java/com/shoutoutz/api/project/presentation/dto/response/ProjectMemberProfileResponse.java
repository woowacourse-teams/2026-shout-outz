package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.net.URI;
import java.util.Map;

/**
 * 프로젝트 팀원 응답 객체
 * 상세 조회와 목록 조회 응답이 함께 쓴다.
 * 가입한 사용자는 avatarUrl, 가입하지 않은 이관 팀원은 githubAvatarUrl과 githubProfileUrl을 가진다.
 */
public record ProjectMemberProfileResponse(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        Integer cohort,
        String track,
        Long avatarImageId,
        String avatarUrl,
        String githubAvatarUrl,
        String githubProfileUrl
) {

    public ProjectMemberProfileResponse(
            String handle,
            String displayName,
            Integer cohort,
            String track,
            Long avatarImageId,
            String avatarUrl,
            String githubAvatarUrl,
            String githubProfileUrl
    ) {
        this(
                null,
                handle,
                displayName,
                UserType.WOOWACOURSE_CREW,
                cohort,
                track,
                avatarImageId,
                avatarUrl,
                githubAvatarUrl,
                githubProfileUrl
        );
    }

    public static ProjectMemberProfileResponse from(
            ProjectMemberProfile member,
            Map<Long, URI> mediaUrls
    ) {
        return new ProjectMemberProfileResponse(
                member.userId(),
                member.handle(),
                member.displayName(),
                member.userType(),
                cohortValue(member.userType(), member.cohort()),
                trackValue(member.userType(), member.track()),
                member.avatarImageId(),
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

    private static Integer cohortValue(UserType userType, Cohort cohort) {
        if (userType != UserType.WOOWACOURSE_CREW || cohort == null) {
            return null;
        }
        return cohort.getValue();
    }

    private static String trackValue(UserType userType, Track track) {
        if (userType != UserType.WOOWACOURSE_CREW || track == null) {
            return null;
        }
        return track.getValue();
    }
}
