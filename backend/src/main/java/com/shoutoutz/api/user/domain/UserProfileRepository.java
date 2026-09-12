package com.shoutoutz.api.user.domain;

import java.util.Optional;

public interface UserProfileRepository {

    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findByUserId(long userId);
}
