package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * judge 판정을 바탕으로 최종 추천 질문을 고른다.
 * hard 탈락 제거 → soft 점수와 생성 순서로 순위 → 중복 제거 → angle 다양성 우선 선택 순서로 동작하며 결정적이다.
 */
public class RecommendationSelector {

    private final double maxCandidateTitleSimilarity;

    public RecommendationSelector(double maxCandidateTitleSimilarity) {
        this.maxCandidateTitleSimilarity = maxCandidateTitleSimilarity;
    }

    public List<RecommendationCandidate> select(List<RecommendationCandidate> candidates, JudgeResult judge,
                                                int maxCount) {
        if (maxCount < 1) {
            throw new IllegalArgumentException("선택할 추천 개수는 1 이상이어야 합니다.");
        }
        Map<String, CandidateVerdict> verdicts = judge.verdicts().stream()
                .collect(Collectors.toMap(CandidateVerdict::candidateId, Function.identity(), (first, second) -> first));

        List<RecommendationCandidate> ranked = candidates.stream()
                .filter(candidate -> verdicts.containsKey(candidate.id()))
                .filter(candidate -> verdicts.get(candidate.id()).passesHardGates())
                .sorted(Comparator.comparingInt((RecommendationCandidate candidate) ->
                                -verdicts.get(candidate.id()).softScore())
                        .thenComparingInt(RecommendationCandidate::order))
                .toList();

        List<RecommendationCandidate> deduped = dedupe(ranked, judge.duplicateGroups());
        Set<RecommendationCandidate> picked = pickWithAngleDiversity(deduped, maxCount);
        return deduped.stream().filter(picked::contains).toList();
    }

    private List<RecommendationCandidate> dedupe(List<RecommendationCandidate> ranked,
                                                 List<List<String>> duplicateGroups) {
        List<RecommendationCandidate> kept = new ArrayList<>();
        for (RecommendationCandidate candidate : ranked) {
            boolean duplicated = kept.stream().anyMatch(keptCandidate ->
                    inSameGroup(candidate.id(), keptCandidate.id(), duplicateGroups)
                            || TextSimilarity.bigramJaccard(candidate.title(), keptCandidate.title())
                            > maxCandidateTitleSimilarity);
            if (!duplicated) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    private boolean inSameGroup(String leftId, String rightId, List<List<String>> duplicateGroups) {
        return duplicateGroups.stream().anyMatch(group -> group.contains(leftId) && group.contains(rightId));
    }

    private Set<RecommendationCandidate> pickWithAngleDiversity(List<RecommendationCandidate> deduped, int maxCount) {
        Set<RecommendationCandidate> picked = new HashSet<>();
        Set<RecommendationAngle> seenAngles = EnumSet.noneOf(RecommendationAngle.class);
        for (RecommendationCandidate candidate : deduped) {
            if (picked.size() < maxCount && seenAngles.add(candidate.angle())) {
                picked.add(candidate);
            }
        }
        for (RecommendationCandidate candidate : deduped) {
            if (picked.size() < maxCount) {
                picked.add(candidate);
            }
        }
        return picked;
    }
}
