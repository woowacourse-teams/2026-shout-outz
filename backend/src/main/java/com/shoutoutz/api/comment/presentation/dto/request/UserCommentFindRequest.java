package com.shoutoutz.api.comment.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 내 댓글 목록 조회 파라미터.
 */
public record UserCommentFindRequest(
        String cursor,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 50, message = "size는 50 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public UserCommentFindRequest {
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
