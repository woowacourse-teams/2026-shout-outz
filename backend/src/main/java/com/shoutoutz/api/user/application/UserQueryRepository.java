package com.shoutoutz.api.user.application;

import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchItem;
import java.util.List;

/** User 조회에 필요한 집계와 다중 테이블 검색을 제공하는 application 포트다. */
public interface UserQueryRepository {

    UserProfileCounts countByUserId(long userId);

    List<UserSearchItem> searchWoowaUsers(
            String keyword,
            UserSearchCursor cursor,
            int limit
    );
}
