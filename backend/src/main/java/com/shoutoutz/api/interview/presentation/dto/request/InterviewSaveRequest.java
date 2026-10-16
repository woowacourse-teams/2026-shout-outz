package com.shoutoutz.api.interview.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.CodePointLength;

public record InterviewSaveRequest(
        @NotBlank @CodePointLength(max = 200) String title,
        @CodePointLength(max = 500) String summary,
        @NotNull @Positive Long intervieweeId,
        @Positive Long thumbnailMediaId,
        @CodePointLength(max = 100000) String contentMd
) {
}
