package com.shoutoutz.api.post.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.post.application.dto.PostSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PostFindAllRequest(
        PostSort sort,

        Long categoryId,

        String cursor,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public PostFindAllRequest {
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    public PostSort resolvedSort() {
        if (sort == null) {
            return PostSort.LATEST;
        }
        return sort;
    }
}
