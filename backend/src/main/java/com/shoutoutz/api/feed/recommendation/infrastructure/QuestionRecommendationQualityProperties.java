package com.shoutoutz.api.feed.recommendation.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 추천 질문 품질 파이프라인 설정. 후보를 candidateCount개 생성해 검증한 뒤 최대 count개, 최소 minCount개를 저장한다.
 */
@ConfigurationProperties(prefix = "question-recommendation.quality")
public record QuestionRecommendationQualityProperties(
        @DefaultValue("8") int candidateCount,
        @DefaultValue("3") int minCount,
        @DefaultValue("3") int minAngleKinds,
        Rules rules
) {

    public QuestionRecommendationQualityProperties {
        if (candidateCount < 1 || candidateCount > 12) {
            throw new IllegalArgumentException("question-recommendation.quality.candidate-count는 1 이상 12 이하여야 합니다.");
        }
        if (minCount < 1 || minCount > candidateCount) {
            throw new IllegalArgumentException("question-recommendation.quality.min-count는 1 이상 candidate-count 이하여야 합니다.");
        }
        if (minAngleKinds < 1 || minAngleKinds > 4) {
            throw new IllegalArgumentException("question-recommendation.quality.min-angle-kinds는 1 이상 4 이하여야 합니다.");
        }
        rules = rules == null ? new Rules(10, 60, 80, 700, 0.4, 0.6, 0.5) : rules;
    }

    public record Rules(
            @DefaultValue("10") int minTitleLength,
            @DefaultValue("60") int maxTitleLength,
            @DefaultValue("80") int minContentLength,
            @DefaultValue("700") int maxContentLength,
            @DefaultValue("0.4") double minKoreanRatio,
            @DefaultValue("0.6") double maxSourceTitleSimilarity,
            @DefaultValue("0.5") double maxCandidateTitleSimilarity
    ) {
        public Rules {
            if (minTitleLength < 1 || maxTitleLength < minTitleLength || maxTitleLength > 100) {
                throw new IllegalArgumentException("추천 제목 길이 규칙은 1 이상 100 이하 범위여야 합니다.");
            }
            if (minContentLength < 1 || maxContentLength < minContentLength || maxContentLength > 5_000) {
                throw new IllegalArgumentException("추천 본문 길이 규칙은 1 이상 5000 이하 범위여야 합니다.");
            }
            if (!isRatio(minKoreanRatio) || !isRatio(maxSourceTitleSimilarity) || !isRatio(maxCandidateTitleSimilarity)) {
                throw new IllegalArgumentException("추천 비율 규칙은 0 이상 1 이하여야 합니다.");
            }
        }

        private static boolean isRatio(double value) {
            return value >= 0 && value <= 1;
        }
    }
}
