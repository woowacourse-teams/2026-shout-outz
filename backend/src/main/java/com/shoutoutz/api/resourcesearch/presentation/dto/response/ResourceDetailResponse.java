package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardLogoUrls;
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
        ResourceCardResponse.Team team,
        ResourceTopicResponse topic,
        List<String> keyPoints,
        List<String> techTags,
        List<ResourceExperienceResponse> experiences,
        ResourceLinkPreviewResponse linkPreview
) {

    public ResourceDetailResponse {
        keyPoints = List.copyOf(keyPoints);
        techTags = List.copyOf(techTags);
        experiences = List.copyOf(experiences);
    }

    public static ResourceDetailResponse from(ResourceDetail detail, ResourceCardLogoUrls logoUrls) {
        ResourceCardResponse card = ResourceCardResponse.from(detail.card(), logoUrls);
        return new ResourceDetailResponse(
                card.resourceId(),
                card.source(),
                card.title(),
                card.url(),
                card.summary(),
                card.publishedAt(),
                card.platform(),
                detail.contentType(),
                card.team(),
                ResourceTopicResponse.from(detail.topic()),
                detail.keyPoints(),
                detail.techTags(),
                detail.experiences().stream().map(ResourceExperienceResponse::from).toList(),
                ResourceLinkPreviewResponse.from(detail.linkPreview())
        );
    }
}
