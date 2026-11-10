package com.shoutoutz.api.resourcesearch.application.dto;

import java.util.List;

public record ResourceDetail(
        ResourceCard card,
        String contentType,
        ResourceTopic topic,
        List<String> keyPoints,
        List<ResourceExperience> experiences
) {

    public ResourceDetail {
        keyPoints = List.copyOf(keyPoints);
        experiences = List.copyOf(experiences);
    }
}
