package com.shoutoutz.api.user.application.query;

import java.util.List;

public record UserSearchResult(
        List<UserSearchItem> items,
        String nextCursor,
        boolean hasNext
) {
}
