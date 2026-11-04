package com.shoutoutz.api.jobposting.presentation.dto.request;

import com.shoutoutz.api.jobposting.domain.ReanalysisScope;
import jakarta.validation.constraints.NotNull;

public record AdminJobPostingReanalysisRequest(
        @NotNull(message = "scope는 필수입니다.")
        ReanalysisScope scope
) {
}
