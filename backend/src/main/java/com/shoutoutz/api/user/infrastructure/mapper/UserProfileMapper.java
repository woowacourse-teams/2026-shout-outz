package com.shoutoutz.api.user.infrastructure.mapper;

import com.shoutoutz.api.user.domain.ProfileDisplayName;
import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.infrastructure.UserProfileEntity;

public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfileEntity toEntity(UserProfile profile) {
        return UserProfileEntity.builder()
                .userId(profile.getUserId())
                .displayName(profile.getDisplayName().value())
                .userType(profile.getUserType())
                .track(profile.getTrack())
                .cohort(profile.getCohort())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .githubProfileUrl(profile.getGithubProfileUrl())
                .blogUrl(profile.getBlogUrl())
                .build();
    }

    public static UserProfile toDomain(UserProfileEntity entity) {
        return UserProfile.builder()
                .userId(entity.getUserId())
                .displayName(new ProfileDisplayName(entity.getDisplayName()))
                .userType(entity.getUserType())
                .track(entity.getTrack())
                .cohort(entity.getCohort())
                .bio(entity.getBio())
                .avatarUrl(entity.getAvatarUrl())
                .githubProfileUrl(entity.getGithubProfileUrl())
                .blogUrl(entity.getBlogUrl())
                .build();
    }
}
