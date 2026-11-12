package com.shoutoutz.api.resourcesearch.presentation.dto.request;

import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResourceRecentRequest(
        @NotNull(message = "kind는 BLOG 또는 TALK여야 합니다.")
        ResourceSection kind,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 20, message = "size는 20 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 4;

    @AssertTrue(message = "kind는 BLOG 또는 TALK여야 합니다.")
    public boolean isKindSupported() {
        return kind != ResourceSection.DOCUMENT;
    }

    public int resolvedSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
