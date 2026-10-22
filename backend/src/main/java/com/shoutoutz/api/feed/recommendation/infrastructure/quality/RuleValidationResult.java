package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import java.util.List;
import java.util.Map;

/**
 * 규칙 검증 결과. passed는 입력 순서를 유지하고, rejected는 후보 id별 첫 번째 위반 사유를 담는다.
 */
public record RuleValidationResult(List<RecommendationCandidate> passed, Map<String, RuleViolation> rejected) {

    public RuleValidationResult {
        passed = List.copyOf(passed);
        rejected = Map.copyOf(rejected);
    }
}
