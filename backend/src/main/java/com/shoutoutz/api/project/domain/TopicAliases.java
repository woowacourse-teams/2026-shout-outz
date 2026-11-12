package com.shoutoutz.api.project.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 프로젝트 탭 검색어를 기술 주제로 해석하는 별칭 사전
 * 별칭은 정규화한 값으로 비교하며, 한 별칭은 한 주제만 가리킨다.
 * 잘못 해석하면 사용자가 찾던 결과가 주제 목록에 가려지므로, 가장 잘 맞는 주제가 하나일 때만 해석한다.
 */
public final class TopicAliases {

    private static final int MIN_LENGTH = 2;
    private static final int CONTAINED_ALIAS_MIN_LENGTH = 3;
    private static final int EXACT = 3;
    private static final int CONTAINS_ALIAS = 2;
    private static final int CONTAINED_IN_ALIAS = 1;

    private final Map<String, String> topicIdByAlias;

    private TopicAliases(Map<String, String> topicIdByAlias) {
        this.topicIdByAlias = Map.copyOf(topicIdByAlias);
    }

    /**
     * 2자 미만인 별칭은 다른 단어에 너무 쉽게 걸려 버린다.
     */
    public static TopicAliases of(Map<String, List<String>> aliasesByTopic, Set<String> catalogTopicIds) {
        Map<String, String> topicIdByAlias = new HashMap<>();
        aliasesByTopic.forEach((topicId, aliases) -> {
            if (!catalogTopicIds.contains(topicId)) {
                throw new IllegalArgumentException("기술 주제 카탈로그에 없는 주제의 별칭입니다: " + topicId);
            }
            for (String alias : aliases) {
                String key = NormalizedKeyword.of(alias).value();
                if (key.length() < MIN_LENGTH) {
                    continue;
                }
                String previous = topicIdByAlias.putIfAbsent(key, topicId);
                if (previous != null && !previous.equals(topicId)) {
                    throw new IllegalArgumentException(
                            "별칭 " + key + " 이 두 주제를 가리킵니다: " + previous + ", " + topicId);
                }
            }
        });
        return new TopicAliases(topicIdByAlias);
    }

    /**
     * 별칭마다 정확히 같으면 3, 검색어가 3자 이상 별칭을 포함하면 2, 별칭이 검색어를 포함하면 1점을 주고
     * 주제의 점수는 그 주제 별칭의 최고 점수다. 최고 점수인 주제가 하나뿐일 때만 그 주제로 해석한다.
     */
    public Optional<String> match(NormalizedKeyword keyword) {
        String value = keyword.value();
        if (value.length() < MIN_LENGTH) {
            return Optional.empty();
        }
        Map<String, Integer> scoreByTopic = new HashMap<>();
        topicIdByAlias.forEach((alias, topicId) -> {
            int score = score(alias, value);
            if (score > 0) {
                scoreByTopic.merge(topicId, score, Math::max);
            }
        });
        int bestScore = scoreByTopic.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> bestTopics = scoreByTopic.entrySet().stream()
                .filter(entry -> entry.getValue() == bestScore)
                .map(Map.Entry::getKey)
                .toList();
        if (bestTopics.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(bestTopics.getFirst());
    }

    private static int score(String alias, String keyword) {
        if (alias.equals(keyword)) {
            return EXACT;
        }
        if (alias.length() >= CONTAINED_ALIAS_MIN_LENGTH && keyword.contains(alias)) {
            return CONTAINS_ALIAS;
        }
        if (alias.contains(keyword)) {
            return CONTAINED_IN_ALIAS;
        }
        return 0;
    }
}
