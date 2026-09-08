package com.shoutoutz.api.user.domain;

public interface UserProfileCountsRepository {

    UserProfileCounts countByUserId(long userId);
}
