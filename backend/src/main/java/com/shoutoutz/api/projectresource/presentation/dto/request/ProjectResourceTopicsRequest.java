package com.shoutoutz.api.projectresource.presentation.dto.request;

import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import jakarta.validation.constraints.Pattern;

/**
 * 기술 이야기 주제 목록 조회 조건. 직군을 생략하면 ALL 이다.
 */
public record ProjectResourceTopicsRequest(
        @Pattern(regexp = "ALL|BE|FE|AN", message = "jobRole은 ALL, BE, FE, AN 중 하나여야 합니다.")
        String jobRole
) {

    public ProjectResourceTopicsRequest {
        jobRole = jobRole == null || jobRole.isBlank() ? null : jobRole.trim();
    }

    public ProjectResourceJobRole resolvedJobRole() {
        return jobRole == null ? ProjectResourceJobRole.ALL : ProjectResourceJobRole.valueOf(jobRole);
    }
}
