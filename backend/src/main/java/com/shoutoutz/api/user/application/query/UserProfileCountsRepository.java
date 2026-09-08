package com.shoutoutz.api.user.application.query;

public interface UserProfileCountsRepository {

    UserProfileCounts countByUserId(long userId);
}
