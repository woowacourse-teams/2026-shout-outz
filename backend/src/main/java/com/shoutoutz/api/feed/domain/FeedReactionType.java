package com.shoutoutz.api.feed.domain;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public enum FeedReactionType {
    LIKE,
    BOOKMARK;

    public static FeedReactionType from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidInputException(FeedErrorCode.REACTION_TYPE_INVALID, exception);
        }
    }
}
