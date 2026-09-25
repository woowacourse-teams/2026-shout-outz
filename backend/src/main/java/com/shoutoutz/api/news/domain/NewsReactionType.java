package com.shoutoutz.api.news.domain;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public enum NewsReactionType {
    LIKE;

    public static NewsReactionType from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidInputException(NewsErrorCode.REACTION_TYPE_INVALID, exception);
        }
    }
}
