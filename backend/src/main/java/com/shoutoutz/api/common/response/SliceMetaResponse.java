package com.shoutoutz.api.common.response;

public record SliceMetaResponse(
        String nextCursor,
        boolean hasNext
) {
}
