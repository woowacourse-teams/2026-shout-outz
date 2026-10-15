package com.shoutoutz.api.feed.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoutoutz.api.feed.recommendation.application.dto.QuestionRecommendationSlot;
import java.util.List;

public record FeedFindAllMetaResponse(
        String nextCursor,
        boolean hasNext,
        long totalCount,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<QuestionRecommendationSlot> recommendationSlots
) {

    public FeedFindAllMetaResponse {
        recommendationSlots = recommendationSlots == null ? List.of() : List.copyOf(recommendationSlots);
    }
}
