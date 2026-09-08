package com.shoutoutz.api.user.presentation.dto.response;

import com.shoutoutz.api.user.application.dto.result.UserSearchResult;

public record UserSearchMetaResponse(
        String nextCursor,
        boolean hasNext
) {

    public static UserSearchMetaResponse from(UserSearchResult result) {
        return new UserSearchMetaResponse(result.nextCursor(), result.hasNext());
    }
}
