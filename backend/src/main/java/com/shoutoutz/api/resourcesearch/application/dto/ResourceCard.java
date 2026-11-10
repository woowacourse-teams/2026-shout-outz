package com.shoutoutz.api.resourcesearch.application.dto;

import java.time.Instant;

public record ResourceCard(
        long resourceId,
        String source,
        String title,
        String url,
        String summary,
        Instant publishedAt,
        String platform,
        ResourceTeam team
) {
}
