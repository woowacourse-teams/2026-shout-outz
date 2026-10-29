package com.shoutoutz.api.feed.recommendation.domain;

public enum QuestionRecommendationJobKind {
    GENERATE,
    REPLENISH;

    public static QuestionRecommendationJobKind fromNullable(QuestionRecommendationJobKind kind) {
        return kind == null ? GENERATE : kind;
    }
}
