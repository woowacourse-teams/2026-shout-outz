package com.shoutoutz.api.user.application;

import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.application.dto.UserSearchCursor;
import com.shoutoutz.api.user.application.dto.UserSearchPage;

/**
 * User 집계와 검색을 위한 조회 포트.
 */
public interface UserQueryRepository {

    /**
     * @param ownerView 본인 조회 여부. true이면 승인 대기 프로젝트와 익명 피드도 센다.
     */
    UserProfileCounts countByUserId(long userId, boolean ownerView);

    UserSearchPage searchWoowaMember(
            String keyword,
            UserSearchCursor cursor,
            int size
    );
}
