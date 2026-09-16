package com.shoutoutz.api.user.domain.profile;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.util.DataResolveUtil;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 공개 정보와 우아한테크코스 인증 정보.
 */
@Getter
public class UserProfile {

    private final Long userId;
    private final ProfileDisplayName displayName;
    private final UserType userType;
    private final Track track;
    private final Cohort cohort;
    private final String bio;
    private final Long avatarImageId;
    private final String githubProfileUrl;
    private final String blogUrl;

    @Builder
    private UserProfile(
            Long userId,
            String displayName,
            UserType userType,
            Track track,
            Cohort cohort,
            String bio,
            Long avatarImageId,
            String githubProfileUrl,
            String blogUrl
    ) {
        String sanitizedBio = DataResolveUtil.sanitizeString(bio);
        String sanitizedGithubProfileUrl = DataResolveUtil.sanitizeString(githubProfileUrl);
        String sanitizedBlogUrl = DataResolveUtil.sanitizeString(blogUrl);

        UserProfileValidator.validateProfile(
                userId,
                userType,
                track,
                cohort
        );
        this.userId = userId;
        this.displayName = new ProfileDisplayName(displayName);
        this.userType = userType;
        this.track = track;
        this.cohort = cohort;
        this.bio = sanitizedBio;
        this.avatarImageId = avatarImageId;
        this.githubProfileUrl = sanitizedGithubProfileUrl;
        this.blogUrl = sanitizedBlogUrl;
    }

    /**
     * 수정 가능한 프로필 정보 갱신.
     * 인증된 우테코 사용자의 표시 이름 변경은 허용하지 않음.
     */
    public UserProfile update(
            String displayName,
            String bio,
            Long avatarImageId,
            String githubProfileUrl,
            String blogUrl
    ) {
        String sanitizedDisplayName = DataResolveUtil.sanitizeString(displayName);
        UserProfileValidator.validateDisplayNameChange(
                userType,
                this.displayName.value(),
                sanitizedDisplayName
        );

        return new UserProfile(
                userId,
                sanitizedDisplayName,
                userType,
                track,
                cohort,
                bio,
                avatarImageId,
                githubProfileUrl,
                blogUrl
        );
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

}
