package com.shoutoutz.api.user.application.query;

import java.util.List;

public interface UserQueryRepository {

    UserProfileCounts countByUserId(long userId);

    List<UserSearchItem> searchCrew(
            String keyword,
            UserSearchCursor cursor,
            int limit
    );
}
