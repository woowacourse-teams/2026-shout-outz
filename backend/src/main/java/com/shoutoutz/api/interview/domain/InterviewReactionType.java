package com.shoutoutz.api.interview.domain;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public enum InterviewReactionType {
    LIKE;

    public static InterviewReactionType from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidInputException(InterviewErrorCode.REACTION_TYPE_INVALID, exception);
        }
    }
}
