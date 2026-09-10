package com.shoutoutz.api.user.presentation.dto.request;

import com.shoutoutz.api.common.validator.CodePointSize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UserSearchRequest(
        @NotBlank(message = "keyword는 필수입니다.")
        @CodePointSize(max = 50, message = "keyword는 50자를 초과할 수 없습니다.")
        String keyword,

        String cursor,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public UserSearchRequest {
        keyword = keyword == null ? null : keyword.trim();
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
