package com.shoutoutz.api.user.application;

import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import java.util.List;

public interface UserQueryRepository {

    UserProfileCounts countByUserId(long userId);

    List<UserSearchItem> searchWoowaUsers(
            String keyword,
            UserSearchCursor cursor,
            int limit
    );
}
