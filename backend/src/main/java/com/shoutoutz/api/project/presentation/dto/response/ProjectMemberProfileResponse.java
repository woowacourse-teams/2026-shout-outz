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
 * 가입한 사용자는 직접 업로드한 이미지가 없으면 GitHub 프로필 이미지 URL을 기본값으로 사용하고,
 * 가입하지 않은 이관 팀원은 저장된 GitHub 프로필 이미지 URL을 내려준다.
 * 이관 팀원은 githubProfileUrl도 함께 가진다.
 */
public record ProjectMemberProfileResponse(
        Long userId,
        String handle,
        String displayName,
        UserType userType,
        Integer cohort,
        String track,
        String avatarUrl,
        String githubProfileUrl
) {

    public ProjectMemberProfileResponse(
            String handle,
            String displayName,
            Integer cohort,
            String track,
            String avatarUrl,
            String githubProfileUrl
    ) {
        this(
                null,
                handle,
                displayName,
                UserType.WOOWACOURSE_CREW,
                cohort,
                track,
                avatarUrl,
                githubProfileUrl
        );
    }

    public static ProjectMemberProfileResponse from(
            ProjectMemberProfile member,
            Map<Long, URI> mediaUrls
    ) {
        return from(member, mediaUrls, Map.of());
    }

    public static ProjectMemberProfileResponse from(
            ProjectMemberProfile member,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        return new ProjectMemberProfileResponse(
                member.userId(),
                member.handle(),
                member.displayName(),
                member.userType(),
                cohortValue(member.userType(), member.cohort()),
                trackValue(member.userType(), member.track()),
                avatarUrl(member, mediaUrls, userAvatarUrls),
                member.githubProfileUrl()
        );
    }

    /**
     * 가입하지 않은 이관 팀원은 저장된 GitHub 프로필 이미지를 보여준다.
     * 가입한 사용자는 직접 업로드한 이미지가 없으면 OAuth 계정의 GitHub 이미지를 보여준다.
     */
    private static String avatarUrl(
            ProjectMemberProfile member,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        if (member.userId() == null) {
            return member.githubAvatarUrl();
        }
        if (member.userType() == null) {
            return null;
        }
        String mediaUrl = toUrl(mediaUrls, member.avatarImageId());
        if (mediaUrl != null) {
            return mediaUrl;
        }
        return userAvatarUrls == null ? null : userAvatarUrls.get(member.userId());
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
