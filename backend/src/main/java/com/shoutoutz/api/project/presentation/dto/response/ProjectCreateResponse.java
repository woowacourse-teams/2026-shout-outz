package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.application.dto.result.ProjectCreateResult;

public record ProjectCreateResponse(
        Long projectId,
        String slug
) {

    public static ProjectCreateResponse from(ProjectCreateResult result) {
        return new ProjectCreateResponse(result.projectId(), result.slug());
    }
}
