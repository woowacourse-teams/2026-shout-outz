package com.shoutoutz.api.feed.recommendation.application;

public class QuestionRecommendationGenerationException extends RuntimeException {

    private final boolean retryable;
    private final long retryAfterSeconds;

    public QuestionRecommendationGenerationException(boolean retryable, long retryAfterSeconds) {
        super("질문 추천을 생성하지 못했습니다.");
        this.retryable = retryable;
        this.retryAfterSeconds = Math.max(0, retryAfterSeconds);
    }

    public boolean retryable() {
        return retryable;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
