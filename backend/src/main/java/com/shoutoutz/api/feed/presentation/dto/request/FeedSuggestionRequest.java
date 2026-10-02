package com.shoutoutz.api.feed.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record FeedSuggestionRequest(
        @NotBlank(message = "keyword는 필수입니다.")
        @CodePointLength(
                min = 2,
                max = 100,
                message = "keyword는 2자 이상 100자 이하여야 합니다."
        )
        String keyword,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 10, message = "size는 10 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 10;

    public FeedSuggestionRequest {
        keyword = DataResolveUtil.sanitizeString(keyword);
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
