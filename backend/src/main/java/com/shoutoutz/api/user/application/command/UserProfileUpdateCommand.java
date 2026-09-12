package com.shoutoutz.api.user.application.command;

public record UserProfileUpdateCommand(
        long userId,
        String displayName,
        String bio,
        Long avatarImageId,
        String githubProfileUrl,
        String blogUrl
) {
}
