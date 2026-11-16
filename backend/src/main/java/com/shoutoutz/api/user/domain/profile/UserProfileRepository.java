package com.shoutoutz.api.user.domain.profile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository {

    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findByUserId(long userId);

    /**
     * 여러 사용자의 프로필을 한 번에 읽는다. 프로필이 없는 사용자는 결과에서 빠지고, 순서는 보장하지 않는다.
     */
    List<UserProfile> findAllByUserIds(Collection<Long> userIds);
}
