package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceTeam;

public record ResourceTeamResponse(String name, String projectSlug, Long logoMediaId) {

    public static ResourceTeamResponse from(ResourceTeam team) {
        if (team == null) {
            return null;
        }
        return new ResourceTeamResponse(team.name(), team.projectSlug(), team.logoMediaId());
    }
}
