package com.shoutoutz.api.user.application.dto;

import java.util.List;

public record UserSearchPage(
        List<UserSearchItem> items,
        boolean hasNext,
        long totalCount
) {

    public UserSearchPage {
        items = List.copyOf(items);
    }
}
