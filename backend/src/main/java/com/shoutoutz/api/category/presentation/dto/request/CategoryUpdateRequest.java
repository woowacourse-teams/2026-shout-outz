package com.shoutoutz.api.category.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.CodePointLength;

public record CategoryUpdateRequest(
        @NotBlank(message = "displayName은 필수입니다.")
        @CodePointLength(max = 50, message = "displayName은 50자를 초과할 수 없습니다.")
        String displayName,

        @NotNull(message = "displayOrder는 필수입니다.")
        @Min(value = 0, message = "displayOrder는 0 이상이어야 합니다.")
        @Max(value = Short.MAX_VALUE, message = "displayOrder는 32767 이하여야 합니다.")
        Integer displayOrder
) {
}
