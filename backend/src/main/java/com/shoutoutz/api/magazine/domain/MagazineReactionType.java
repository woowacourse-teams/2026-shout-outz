package com.shoutoutz.api.magazine.domain;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public enum MagazineReactionType {
    LIKE;

    public static MagazineReactionType from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidInputException(MagazineErrorCode.REACTION_TYPE_INVALID, exception);
        }
    }
}
