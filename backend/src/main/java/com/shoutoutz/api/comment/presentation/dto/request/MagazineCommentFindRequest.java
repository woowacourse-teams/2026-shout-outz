package com.shoutoutz.api.comment.presentation.dto.request;

import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.MagazineCommentSort;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;

public record MagazineCommentFindRequest(
        String cursor,
        int size,
        MagazineCommentSort sort
) {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    public MagazineCommentFindRequest {
        cursor = normalizeCursor(cursor);
        size = validateSize(size);
        if (sort == null) {
            sort = MagazineCommentSort.LATEST;
        }
    }

    public MagazineCommentFindRequest(String cursor, int size, String sort) {
        this(cursor, size, validateSort(sort));
    }

    private static String normalizeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return cursor;
    }

    private static int validateSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_SIZE);
        }
        return size;
    }

    private static MagazineCommentSort validateSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return MagazineCommentSort.LATEST;
        }
        try {
            return MagazineCommentSort.valueOf(sort);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_SORT, exception);
        }
    }
}
