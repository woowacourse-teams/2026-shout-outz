package com.shoutoutz.api.comment.presentation.dto.request;

import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.ProjectCommentSort;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public record ProjectCommentFindRequest(
        String cursor,
        int size,
        ProjectCommentSort sort
) {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    public ProjectCommentFindRequest {
        cursor = normalizeCursor(cursor);
        size = validateSize(size);
        sort = sort == null ? ProjectCommentSort.LATEST : sort;
    }

    public ProjectCommentFindRequest(String cursor, int size, String sort) {
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

    private static ProjectCommentSort validateSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return ProjectCommentSort.LATEST;
        }
        try {
            return ProjectCommentSort.valueOf(sort);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_SORT, exception);
        }
    }
}
