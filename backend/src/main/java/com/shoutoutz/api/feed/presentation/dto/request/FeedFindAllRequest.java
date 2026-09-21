package com.shoutoutz.api.feed.presentation.dto.request;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.CodePointLength;

public record FeedFindAllRequest(
        FeedSort sort,

        Long categoryId,

        @CodePointLength(max = 100, message = "keyword는 100자를 초과할 수 없습니다.")
        String keyword,

        String cursor,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public FeedFindAllRequest {
        keyword = DataResolveUtil.sanitizeString(keyword);
        cursor = DataResolveUtil.sanitizeString(cursor);
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    public FeedSort resolvedSort() {
        if (keyword != null) {
            if (sort == null || sort == FeedSort.RELEVANCE) {
                return FeedSort.RELEVANCE;
            }
            throw new BadRequestException(FeedErrorCode.FEED_SEARCH_SORT_INVALID);
        }
        if (sort == FeedSort.RELEVANCE) {
            throw new BadRequestException(FeedErrorCode.FEED_SEARCH_SORT_INVALID);
        }
        if (sort == null) {
            return FeedSort.LATEST;
        }
        return sort;
    }
}
