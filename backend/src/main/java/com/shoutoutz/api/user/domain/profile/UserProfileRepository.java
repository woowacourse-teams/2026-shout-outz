package com.shoutoutz.api.user.domain.profile;

import java.util.Optional;

public interface UserProfileRepository {

    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findByUserId(long userId);
}
