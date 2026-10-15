package com.shoutoutz.api.feed.recommendation.application;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;
import org.springframework.stereotype.Component;

@Component
public class QuestionRecommendationRetryPolicy {

    private static final long MAX_BACKOFF_SECONDS = 300;
    private static final long MAX_VISIBILITY_SECONDS = 43_200;

    private final LongUnaryOperator jitter;

    public QuestionRecommendationRetryPolicy() {
        this(bound -> ThreadLocalRandom.current().nextLong(bound));
    }

    QuestionRecommendationRetryPolicy(LongUnaryOperator jitter) {
        this.jitter = jitter;
    }

    public long delaySeconds(int attempts, long retryAfterSeconds) {
        int exponent = Math.max(0, Math.min(attempts - 1, 6));
        long backoff = Math.min(MAX_BACKOFF_SECONDS, 5L << exponent);
        long maxJitter = Math.min(backoff / 5, MAX_BACKOFF_SECONDS - backoff);
        long delay = backoff + jitter.applyAsLong(maxJitter + 1);
        return Math.min(MAX_VISIBILITY_SECONDS, Math.max(delay, retryAfterSeconds));
    }
}
