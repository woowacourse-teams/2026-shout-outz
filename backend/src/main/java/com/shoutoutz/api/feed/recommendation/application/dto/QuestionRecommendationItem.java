package com.shoutoutz.api.feed.recommendation.application.dto;

public record QuestionRecommendationItem(
        long recommendationId,
        String title,
        String content
) {
}
