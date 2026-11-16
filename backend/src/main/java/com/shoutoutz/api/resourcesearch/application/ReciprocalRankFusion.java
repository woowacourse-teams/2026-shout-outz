package com.shoutoutz.api.resourcesearch.application;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 여러 검색의 문서 순위를 순위만으로 합친다(Reciprocal Rank Fusion).
 * 코사인 유사도와 BM25 점수는 범위가 달라 직접 더할 수 없어서, 각 순위 r에 1 / (k + r)을 주고 문서별로 더한다.
 */
final class ReciprocalRankFusion {

    // 순위 차이를 얼마나 완만하게 볼지 정한다. 클수록 1위와 10위의 차이가 줄어든다. 원 논문과 오프라인 비교에서 쓴 값이다.
    private static final int RANK_CONSTANT = 60;

    private ReciprocalRankFusion() {
    }

    static List<Long> fuse(List<List<Long>> rankings) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (List<Long> ranking : rankings) {
            validateDistinct(ranking);
            for (int index = 0; index < ranking.size(); index++) {
                scores.merge(ranking.get(index), 1.0 / (RANK_CONSTANT + index + 1), Double::sum);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void validateDistinct(List<Long> ranking) {
        Set<Long> documentIds = new HashSet<>(ranking);
        if (documentIds.size() != ranking.size()) {
            throw new IllegalArgumentException("한 순위에 같은 문서가 두 번 있을 수 없습니다.");
        }
    }
}
