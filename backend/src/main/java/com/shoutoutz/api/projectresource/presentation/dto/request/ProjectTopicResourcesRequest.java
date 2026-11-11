package com.shoutoutz.api.projectresource.presentation.dto.request;

import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 기술 이야기 주제별 자료 조회 조건. 주제는 이름이 아니라 id(resource_topic_catalog.id)로 받는다.
 * 이름은 바뀔 수 있고 'N+1 문제'의 + 처럼 주소에서 다른 글자로 읽히는 글자가 있어서다.
 */
public record ProjectTopicResourcesRequest(
        @NotBlank(message = "topicId는 필수입니다.")
        @Size(max = 40, message = "topicId는 40자 이하여야 합니다.")
        String topicId,
        @Pattern(regexp = "ALL|BE|FE|AN", message = "jobRole은 ALL, BE, FE, AN 중 하나여야 합니다.")
        String jobRole
) {

    public ProjectTopicResourcesRequest {
        topicId = topicId == null ? null : topicId.trim();
        jobRole = jobRole == null || jobRole.isBlank() ? null : jobRole.trim();
    }

    public ProjectResourceJobRole resolvedJobRole() {
        return jobRole == null ? ProjectResourceJobRole.ALL : ProjectResourceJobRole.valueOf(jobRole);
    }
}
