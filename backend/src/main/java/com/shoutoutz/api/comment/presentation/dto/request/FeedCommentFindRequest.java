package com.shoutoutz.api.comment.presentation.dto.request;

import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public record FeedCommentFindRequest(
        String cursor,
        int size,
        FeedCommentSort sort
) {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    public FeedCommentFindRequest {
        cursor = normalizeCursor(cursor);
        size = validateSize(size);
        sort = sort == null ? FeedCommentSort.LATEST : sort;
    }

    public FeedCommentFindRequest(String cursor, int size, String sort) {
        this(cursor, size, validateSort(sort));
    }

    private static String normalizeCursor(String cursor) {
        return cursor == null || cursor.isBlank() ? null : cursor;
    }

    private static int validateSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_SIZE);
        }
        return size;
    }

    private static FeedCommentSort validateSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return FeedCommentSort.LATEST;
        }
        try {
            return FeedCommentSort.valueOf(sort);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_SORT, exception);
        }
    }
}
