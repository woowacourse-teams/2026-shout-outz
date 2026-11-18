package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;

public record MatchedProjectResponse(
        String name,
        String projectSlug
) {

    public static MatchedProjectResponse from(MatchedProject project) {
        return new MatchedProjectResponse(project.name(), project.slug());
    }
}
