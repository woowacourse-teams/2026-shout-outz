package com.shoutoutz.api.magazine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.CodePointLength;

public record MagazineSaveRequest(
        @NotBlank @CodePointLength(max = 200) String title,
        @CodePointLength(max = 500) String summary,
        @NotNull @Positive Long featuredUserId,
        @CodePointLength(max = 200) String featuredUserIntroduction,
        @Positive Long thumbnailMediaId,
        @CodePointLength(max = 100000) String contentMd
) {
}
