package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.time.Instant;

public record ResourceCardResponse(
        long resourceId,
        String source,
        String title,
        String url,
        String summary,
        Instant publishedAt,
        String platform,
        ResourceTeamResponse team
) {

    public static ResourceCardResponse from(ResourceCard card) {
        return new ResourceCardResponse(
                card.resourceId(),
                card.source(),
                card.title(),
                card.url(),
                card.summary(),
                card.publishedAt(),
                card.platform(),
                ResourceTeamResponse.from(card.team())
        );
    }
}
