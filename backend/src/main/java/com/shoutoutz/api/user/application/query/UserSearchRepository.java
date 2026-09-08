package com.shoutoutz.api.user.application.query;

import java.util.List;

public interface UserSearchRepository {

    List<UserSearchItem> searchCrew(
            String keyword,
            UserSearchCursor cursor,
            int limit
    );
}
