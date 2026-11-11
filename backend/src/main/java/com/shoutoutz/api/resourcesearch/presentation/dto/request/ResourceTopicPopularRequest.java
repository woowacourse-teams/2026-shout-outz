package com.shoutoutz.api.resourcesearch.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ResourceTopicPopularRequest(
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 20, message = "size는 20 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 10;

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
