package com.shoutoutz.api.user.infrastructure.mapper;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.infrastructure.UserProfileEntity;

public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfileEntity toEntity(UserProfile profile) {
        return UserProfileEntity.builder()
                .userId(profile.getUserId())
                .displayName(profile.getDisplayName().value())
                .userType(profile.getUserType())
                .track(toTrackValue(profile))
                .cohort(toCohortValue(profile))
                .bio(profile.getBio())
                .avatarImageId(profile.getAvatarImageId())
                .githubProfileUrl(profile.getGithubProfileUrl())
                .blogUrl(profile.getBlogUrl())
                .build();
    }

    public static UserProfile toDomain(UserProfileEntity entity) {
        return UserProfile.builder()
                .userId(entity.getUserId())
                .displayName(entity.getDisplayName())
                .userType(entity.getUserType())
                .track(toTrack(entity.getTrack()))
                .cohort(toCohort(entity.getCohort()))
                .bio(entity.getBio())
                .avatarImageId(entity.getAvatarImageId())
                .githubProfileUrl(entity.getGithubProfileUrl())
                .blogUrl(entity.getBlogUrl())
                .build();
    }

    private static String toTrackValue(UserProfile profile) {
        if (profile.getTrack() == null) {
            return null;
        }
        return profile.getTrack().getValue();
    }

    private static Short toCohortValue(UserProfile profile) {
        if (profile.getCohort() == null) {
            return null;
        }
        return (short) profile.getCohort().getValue();
    }

    private static Track toTrack(String value) {
        if (value == null) {
            return null;
        }
        return Track.from(value);
    }

    private static Cohort toCohort(Short value) {
        if (value == null) {
            return null;
        }
        return Cohort.from(value);
    }
}
