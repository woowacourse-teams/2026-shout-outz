package com.shoutoutz.api.comment.domain;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public enum ProjectCommentReactionType {
    AGREE;

    public static ProjectCommentReactionType from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidInputException(CommentErrorCode.REACTION_TYPE_INVALID, exception);
        }
    }
}
