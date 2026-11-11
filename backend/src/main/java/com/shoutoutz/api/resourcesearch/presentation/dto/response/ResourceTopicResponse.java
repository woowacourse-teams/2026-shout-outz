package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;

public record ResourceTopicResponse(String topicId, String name) {

    public static ResourceTopicResponse from(ResourceTopic topic) {
        if (topic == null) {
            return null;
        }
        return new ResourceTopicResponse(topic.topicId(), topic.name());
    }
}
