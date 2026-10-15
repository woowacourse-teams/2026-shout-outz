package com.shoutoutz.api.feed.recommendation.application.dto;

import com.shoutoutz.api.feed.recommendation.domain.QuestionRecommendationStatus;
import java.util.List;

public record RelatedQuestionRecommendations(
        QuestionRecommendationStatus status,
        List<QuestionRecommendationItem> items
) {

    public RelatedQuestionRecommendations {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
