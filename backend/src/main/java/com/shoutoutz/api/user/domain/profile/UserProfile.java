package com.shoutoutz.api.user.domain.profile;

import com.shoutoutz.api.common.util.DataResolveUtil;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserProfile {

    private final Long userId;
    private final ProfileDisplayName displayName;
    private final UserType userType;
    private final String track;
    private final Short cohort;
    private final String bio;
    private final Long avatarImageId;
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
            Long avatarImageId,
            String githubProfileUrl,
            String blogUrl
    ) {
        String sanitizedTrack = DataResolveUtil.sanitizeString(track);
        String sanitizedBio = DataResolveUtil.sanitizeString(bio);
        String sanitizedGithubProfileUrl = DataResolveUtil.sanitizeString(githubProfileUrl);
        String sanitizedBlogUrl = DataResolveUtil.sanitizeString(blogUrl);

        UserProfileValidator.validateProfile(
                userId,
                userType,
                sanitizedTrack,
                cohort,
                sanitizedBio,
                avatarImageId,
                sanitizedGithubProfileUrl,
                sanitizedBlogUrl
        );
        this.userId = userId;
        this.displayName = new ProfileDisplayName(displayName);
        this.userType = userType;
        this.track = sanitizedTrack;
        this.cohort = cohort;
        this.bio = sanitizedBio;
        this.avatarImageId = avatarImageId;
        this.githubProfileUrl = sanitizedGithubProfileUrl;
        this.blogUrl = sanitizedBlogUrl;
    }

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

    public boolean canChangeDisplayNameTo(String displayName) {
        String sanitizedDisplayName = DataResolveUtil.sanitizeString(displayName);
        return userType == UserType.GENERAL || this.displayName.value().equals(sanitizedDisplayName);
    }

    public static UserProfile initialize(Long userId, String displayName) {
        return initialize(
                userId,
                displayName,
                UserType.GENERAL,
                null,
                null,
                null
        );
    }

    public static UserProfile initialize(
            Long userId,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String githubProfileUrl
    ) {
        return new UserProfile(
                userId,
                displayName,
                userType,
                track,
                cohort,
                null,
                null,
                githubProfileUrl,
                null
        );
    }

}
