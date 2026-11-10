package com.shoutoutz.api.resourcesearch.application.dto;

import java.util.List;

public record ResourceDetail(
        ResourceCard card,
        String contentType,
        ResourceTopic topic,
        List<String> keyPoints,
        List<String> techTags,
        List<ResourceExperience> experiences
) {

    public ResourceDetail {
        keyPoints = List.copyOf(keyPoints);
        techTags = List.copyOf(techTags);
        experiences = List.copyOf(experiences);
    }
}
