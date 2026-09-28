package com.shoutoutz.api.user.application;

import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import java.util.List;

/**
 * User 집계와 검색을 위한 조회 포트.
 */
public interface UserQueryRepository {

    UserProfileCounts countByUserId(long userId);

    UserProfileCounts countByUserId(long userId, boolean includePending);

    List<UserSearchItem> searchWoowaMember(
            String keyword,
            UserSearchCursor cursor,
            int limit
    );
}
