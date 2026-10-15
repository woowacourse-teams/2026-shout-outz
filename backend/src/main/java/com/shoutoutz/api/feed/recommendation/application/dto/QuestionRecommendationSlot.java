package com.shoutoutz.api.feed.recommendation.application.dto;

public record QuestionRecommendationSlot(
        long afterFeedId,
        QuestionRecommendationItem recommendation
) {
}
