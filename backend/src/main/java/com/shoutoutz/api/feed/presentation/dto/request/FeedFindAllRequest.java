package com.shoutoutz.api.feed.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FeedFindAllRequest(
        FeedSort sort,

        Long categoryId,

        String cursor,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public FeedFindAllRequest {
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    public FeedSort resolvedSort() {
        if (sort == null) {
            return FeedSort.LATEST;
        }
        return sort;
    }
}
