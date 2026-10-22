package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationQualityProperties;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationQualityTypesTest {

    @Test
    void 체크리스트는_hard_7개와_soft_4개로_나뉜다() {
        assertThat(RecommendationCheck.hardGates()).hasSize(7);
        assertThat(RecommendationCheck.softChecks()).hasSize(4);
    }

    @Test
    void 모든_hard_항목을_통과해야_판정을_통과한다() {
        Map<RecommendationCheck, Boolean> checks = allPassed();
        assertThat(new CandidateVerdict("c1", checks).passesHardGates()).isTrue();
        assertThat(new CandidateVerdict("c1", checks).softScore()).isEqualTo(4);

        checks.put(RecommendationCheck.SAFE, false);
        checks.put(RecommendationCheck.SPECIFIC, false);
        CandidateVerdict failed = new CandidateVerdict("c1", checks);
        assertThat(failed.passesHardGates()).isFalse();
        assertThat(failed.softScore()).isEqualTo(3);
    }

    @Test
    void 항목이_빠진_판정은_거부한다() {
        Map<RecommendationCheck, Boolean> checks = allPassed();
        checks.remove(RecommendationCheck.TITLE_MATCH);

        assertThatThrownBy(() -> new CandidateVerdict("c1", checks)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bigram_유사도는_공백과_기호를_무시한다() {
        assertThat(TextSimilarity.bigramJaccard("캐시 정합성!", "캐시정합성")).isEqualTo(1.0);
        assertThat(TextSimilarity.bigramJaccard("캐시 정합성", "스레드 풀")).isZero();
        assertThat(TextSimilarity.bigramJaccard("", "")).isZero();
    }

    @Test
    void 품질_설정은_후보_수와_최소_개수의_범위를_검증한다() {
        assertThat(new QuestionRecommendationQualityProperties(8, 3, 3, null).rules().maxContentLength())
                .isEqualTo(700);
        assertThatThrownBy(() -> new QuestionRecommendationQualityProperties(2, 3, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuestionRecommendationQualityProperties(8, 3, 5, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Map<RecommendationCheck, Boolean> allPassed() {
        Map<RecommendationCheck, Boolean> checks = new EnumMap<>(RecommendationCheck.class);
        Arrays.stream(RecommendationCheck.values()).forEach(check -> checks.put(check, true));
        return checks;
    }
}
