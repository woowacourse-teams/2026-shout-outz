package com.shoutoutz.api.user.application.dto.result;

import com.shoutoutz.api.user.application.query.UserSearchItem;
import java.util.List;

public record UserSearchResult(
        List<UserSearchItem> items,
        String nextCursor,
        boolean hasNext
) {
}
