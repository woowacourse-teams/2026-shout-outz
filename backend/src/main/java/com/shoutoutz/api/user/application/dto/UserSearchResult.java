package com.shoutoutz.api.user.application.dto;

import java.util.List;

public record UserSearchResult(
        List<UserSearchItem> items,
        String nextCursor,
        boolean hasNext
) {
}
