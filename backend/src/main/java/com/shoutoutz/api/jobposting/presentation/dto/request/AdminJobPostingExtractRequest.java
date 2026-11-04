package com.shoutoutz.api.jobposting.presentation.dto.request;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.CodePointLength;

public record AdminJobPostingExtractRequest(
        @NotBlank(message = "sourceUrl은 필수입니다.")
        @CodePointLength(max = JobPostingUrl.MAX_LENGTH, message = "sourceUrl은 1,024자를 초과할 수 없습니다.")
        @Pattern(regexp = JobPostingUrl.REGEX, message = "sourceUrl은 http 또는 https URL 형식이어야 합니다.")
        String sourceUrl,

        @NotNull(message = "content는 필수입니다.")
        @Pattern(regexp = AdminJobPostingSaveRequest.HAS_TEXT, message = "content는 필수입니다.")
        @CodePointLength(max = AdminJobPostingSaveRequest.MAX_CONTENT_LENGTH, message = "content는 10,000자를 초과할 수 없습니다.")
        String content
) {

    /**
     * 공고 원문은 해시를 만들 때 공백을 따로 정리하므로 그대로 둔다.
     */
    public AdminJobPostingExtractRequest {
        sourceUrl = DataResolveUtil.sanitizeString(sourceUrl);
    }
}
