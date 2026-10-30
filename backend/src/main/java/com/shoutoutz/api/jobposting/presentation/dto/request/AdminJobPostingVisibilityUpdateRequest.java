package com.shoutoutz.api.jobposting.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminJobPostingVisibilityUpdateRequest(
        @NotNull(message = "hidden은 필수입니다.")
        Boolean hidden
) {
}
