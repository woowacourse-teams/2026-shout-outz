package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TopicAliasesTest {

    private static final Map<String, List<String>> ALIASES = Map.of(
            "concurrency", List.of("동시성 제어/락", "동시성", "lock", "분산 락"),
            "cache", List.of("캐시(Redis 등)", "redis", "캐시"),
            "auth", List.of("인증/인가", "Spring Security", "jwt", "세션"),
            "unit_test", List.of("단위 테스트"),
            "integration_test", List.of("통합 테스트")
    );

    private final TopicAliases topicAliases = TopicAliases.of(ALIASES, ALIASES.keySet());

    @ParameterizedTest
    @CsvSource({
            "동시성, concurrency",
            "'SPRING  security', auth",
            "'캐시 (Redis 등)', cache",
            "'동시성 문제 해결', concurrency",
            "동시, concurrency"
    })
    @DisplayName("별칭과 같거나, 3자 이상 별칭을 포함하거나, 별칭에 포함되면 가장 잘 맞는 주제 하나로 해석한다.")
    void matchesBestSingleTopic(String keyword, String topicId) {
        assertThat(topicAliases.match(NormalizedKeyword.of(keyword))).contains(topicId);
    }

    @Test
    @DisplayName("정확히 같은 별칭은 다른 주제의 부분 일치보다 우선한다.")
    void prefersExactAliasOverPartialMatch() {
        Map<String, List<String>> aliases = Map.of(
                "cache", List.of("redis"),
                "concurrency", List.of("redis lock")
        );

        TopicAliases redis = TopicAliases.of(aliases, aliases.keySet());

        assertThat(redis.match(NormalizedKeyword.of("redis"))).contains("cache");
    }

    @ParameterizedTest
    @ValueSource(strings = {"redis 동시성", "테스트", "eda", "락"})
    @DisplayName("가장 잘 맞는 주제가 여러 개이거나, 맞는 주제가 없거나, 비교할 글자가 2자 미만이면 해석하지 않는다.")
    void doesNotMatchAmbiguousUnknownOrTooShortKeyword(String keyword) {
        assertThat(topicAliases.match(NormalizedKeyword.of(keyword))).isEmpty();
    }

    @Test
    @DisplayName("같은 별칭이 두 주제를 가리키면 만들 수 없다.")
    void rejectsAliasSharedByTopics() {
        Map<String, List<String>> aliases = Map.of(
                "deploy", List.of("롤백"),
                "incident", List.of("롤 백")
        );

        assertThatThrownBy(() -> TopicAliases.of(aliases, aliases.keySet()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("롤백");
    }

    @Test
    @DisplayName("기술 주제 카탈로그에 없는 주제의 별칭은 만들 수 없다.")
    void rejectsUnknownTopic() {
        assertThatThrownBy(() -> TopicAliases.of(ALIASES, Set.of("concurrency", "cache", "auth", "unit_test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integration_test");
    }
}
