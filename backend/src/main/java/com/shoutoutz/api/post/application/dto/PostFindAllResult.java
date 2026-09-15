package com.shoutoutz.api.post.application.dto;

import java.util.List;

public record PostFindAllResult(
        List<PostItem> items,
        String nextCursor,
        boolean hasNext
) {
}
