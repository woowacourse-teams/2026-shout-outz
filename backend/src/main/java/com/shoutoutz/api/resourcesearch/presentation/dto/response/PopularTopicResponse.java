package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import com.shoutoutz.api.resourcesearch.application.dto.PopularTopic;

public record PopularTopicResponse(String topicId, String name, long resourceCount) {

    public static PopularTopicResponse from(PopularTopic popularTopic) {
        return new PopularTopicResponse(
                popularTopic.topic().topicId(),
                popularTopic.topic().name(),
                popularTopic.resourceCount()
        );
    }
}
