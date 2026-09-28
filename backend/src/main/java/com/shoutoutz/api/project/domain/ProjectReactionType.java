package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public enum ProjectReactionType {
    LIKE,
    BOOKMARK;

    public static ProjectReactionType from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidInputException(ProjectErrorCode.REACTION_TYPE_INVALID, exception);
        }
    }
}
