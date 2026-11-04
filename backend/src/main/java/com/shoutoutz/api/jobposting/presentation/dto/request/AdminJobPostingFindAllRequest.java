package com.shoutoutz.api.jobposting.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminJobPostingFindAllRequest(
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        String cursor
) {

    private static final int DEFAULT_SIZE = 20;

    public AdminJobPostingFindAllRequest {
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
