package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import java.util.List;

public record ResourceTopicSideResponse(ResourceTopicSide side, List<ResourceTopicResponse> topics) {

    public ResourceTopicSideResponse {
        topics = List.copyOf(topics);
    }
}
