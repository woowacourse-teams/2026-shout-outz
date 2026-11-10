package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceDetail;
import java.time.Instant;
import java.util.List;

public record ResourceDetailResponse(
        long resourceId,
        String source,
        String title,
        String url,
        String summary,
        Instant publishedAt,
        String platform,
        String contentType,
        ResourceTeamResponse team,
        ResourceTopicResponse topic,
        List<String> keyPoints,
        List<ResourceExperienceResponse> experiences
) {

    public ResourceDetailResponse {
        keyPoints = List.copyOf(keyPoints);
        experiences = List.copyOf(experiences);
    }

    public static ResourceDetailResponse from(ResourceDetail detail) {
        ResourceCard card = detail.card();
        return new ResourceDetailResponse(
                card.resourceId(),
                card.source(),
                card.title(),
                card.url(),
                card.summary(),
                card.publishedAt(),
                card.platform(),
                detail.contentType(),
                ResourceTeamResponse.from(card.team()),
                ResourceTopicResponse.from(detail.topic()),
                detail.keyPoints(),
                detail.experiences().stream().map(ResourceExperienceResponse::from).toList()
        );
    }
}
