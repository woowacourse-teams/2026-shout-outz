package com.shoutoutz.api.comment.application.dto;

import java.util.List;

public record UserCommentPage(
        List<UserCommentItem> items,
        boolean hasNext,
        long totalCount
) {

    public UserCommentPage {
        items = List.copyOf(items);
    }
}
