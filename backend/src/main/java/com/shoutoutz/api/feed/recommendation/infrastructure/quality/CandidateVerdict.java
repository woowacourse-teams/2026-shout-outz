package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * judge가 후보 하나에 내린 판정. 모든 체크 항목의 결과를 가져야 한다.
 */
public record CandidateVerdict(String candidateId, Map<RecommendationCheck, Boolean> checks) {

    public CandidateVerdict {
        if (candidateId == null || checks == null || checks.size() != RecommendationCheck.values().length
                || checks.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("판정에는 후보 id와 모든 체크 항목의 결과가 있어야 합니다.");
        }
        checks = Map.copyOf(new EnumMap<>(checks));
    }

    public boolean passesHardGates() {
        return RecommendationCheck.hardGates().stream().allMatch(checks::get);
    }

    public int softScore() {
        return (int) RecommendationCheck.softChecks().stream().filter(checks::get).count();
    }
}
