package com.shoutoutz.api.feed.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.feed.domain.FeedType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 사용자 피드 목록 조회 파라미터.
 */
public record UserFeedFindRequest(
        String cursor,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 50, message = "size는 50 이하여야 합니다.")
        Integer size,

        FeedType type
) {

    private static final int DEFAULT_SIZE = 20;

    public UserFeedFindRequest {
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public UserFeedFindRequest(String cursor, Integer size) {
        this(cursor, size, null);
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
