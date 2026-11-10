package com.shoutoutz.api.resourcesearch.application.dto;

public record ResourceTeam(String name, String projectSlug, Long logoMediaId) {

    public static ResourceTeam resolve(
            String projectTeamName,
            String projectSlug,
            Long logoMediaId,
            String collectedTeamName
    ) {
        if (projectSlug != null) {
            return new ResourceTeam(projectTeamName, projectSlug, logoMediaId);
        }
        if (collectedTeamName == null || collectedTeamName.isBlank()) {
            return null;
        }
        return new ResourceTeam(collectedTeamName, null, null);
    }
}
