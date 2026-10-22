package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import static com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationAngle.ADJACENT;
import static com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationAngle.COMPARE;
import static com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationAngle.DEEPEN;
import static com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationAngle.PRACTICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationSelectorTest {

    private final RecommendationSelector selector = new RecommendationSelector(0.6);

    @Test
    void hard_항목에_실패한_후보는_제외한다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, ADJACENT, "캐시 무효화 시점 결정 방법"));
        JudgeResult judge = judge(
                verdict("c1", RecommendationCheck.NO_FABRICATION),
                verdict("c2"));

        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c2");
    }

    @Test
    void 판정이_없는_후보는_제외한다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, ADJACENT, "캐시 무효화 시점 결정 방법"));

        assertThat(ids(selector.select(candidates, judge(verdict("c2")), 3))).containsExactly("c2");
    }

    @Test
    void soft_점수가_높은_순으로_고르고_동점이면_생성_순서를_따른다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, ADJACENT, "캐시 무효화 시점 결정 방법"),
                candidate("c3", 3, COMPARE, "모놀리스와 MSA 전환 판단"));
        JudgeResult judge = judge(
                verdict("c1", RecommendationCheck.SPECIFIC),
                verdict("c2"),
                verdict("c3"));

        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c2", "c3", "c1");
    }

    @Test
    void 중복_묶음에서는_가장_순위가_높은_후보만_남긴다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, ADJACENT, "캐시 무효화 시점 결정 방법"),
                candidate("c3", 3, COMPARE, "모놀리스와 MSA 전환 판단"));
        JudgeResult judge = new JudgeResult(
                List.of(verdict("c1", RecommendationCheck.TITLE_MATCH), verdict("c2"), verdict("c3")),
                List.of(List.of("c1", "c2")));

        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c2", "c3");
    }

    @Test
    void 제목이_비슷한_후보는_순위가_낮은_쪽을_제외한다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, ADJACENT, "트랜잭션 격리 수준 선택 기준은?"),
                candidate("c3", 3, COMPARE, "모놀리스와 MSA 전환 판단"));
        JudgeResult judge = judge(verdict("c1"), verdict("c2"), verdict("c3"));

        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c1", "c3");
    }

    @Test
    void 개수가_제한되면_점수가_높은_같은_angle보다_다른_angle을_우선한다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, DEEPEN, "캐시 무효화 시점 결정 방법"),
                candidate("c3", 3, DEEPEN, "모놀리스와 MSA 전환 판단"),
                candidate("c4", 4, DEEPEN, "인덱스 설계 시 카디널리티 고려"),
                candidate("c5", 5, PRACTICE, "배포 롤백 절차 정리 경험"));
        JudgeResult judge = judge(
                verdict("c1"), verdict("c2"), verdict("c3"), verdict("c4"),
                verdict("c5", RecommendationCheck.SPECIFIC, RecommendationCheck.NATURAL_VOICE));

        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c1", "c2", "c5");
    }

    @Test
    void angle별_대표를_고른_뒤_남은_자리는_순위대로_채운다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, DEEPEN, "캐시 무효화 시점 결정 방법"),
                candidate("c3", 3, DEEPEN, "모놀리스와 MSA 전환 판단"),
                candidate("c4", 4, COMPARE, "인덱스 설계 시 카디널리티 고려"));
        JudgeResult judge = judge(verdict("c1"), verdict("c2"), verdict("c3"),
                verdict("c4", RecommendationCheck.SPECIFIC));

        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c1", "c2", "c4");
    }

    @Test
    void 남은_후보가_부족하면_maxCount보다_적게_반환한다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, ADJACENT, "캐시 무효화 시점 결정 방법"));
        JudgeResult judge = judge(verdict("c1"), verdict("c2", RecommendationCheck.SAFE));

        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c1");
    }

    @Test
    void 결과는_선택_순서가_아닌_순위_순서로_반환한다() {
        List<RecommendationCandidate> candidates = List.of(
                candidate("c1", 1, DEEPEN, "트랜잭션 격리 수준 선택 기준"),
                candidate("c2", 2, DEEPEN, "캐시 무효화 시점 결정 방법"),
                candidate("c3", 3, PRACTICE, "모놀리스와 MSA 전환 판단"));
        JudgeResult judge = judge(verdict("c1"), verdict("c2"),
                verdict("c3", RecommendationCheck.SPECIFIC));

        // 다양성 단계에서 c1, c3를 먼저 고르고 c2로 채우지만 결과는 순위 순서다.
        assertThat(ids(selector.select(candidates, judge, 3))).containsExactly("c1", "c2", "c3");
    }

    @Test
    void maxCount가_1보다_작으면_예외가_발생한다() {
        assertThatThrownBy(() -> selector.select(List.of(), judge(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static RecommendationCandidate candidate(String id, int order, RecommendationAngle angle, String title) {
        return new RecommendationCandidate(id, order, angle, title, title + " 본문");
    }

    private static CandidateVerdict verdict(String id, RecommendationCheck... failed) {
        Map<RecommendationCheck, Boolean> checks = new EnumMap<>(RecommendationCheck.class);
        Arrays.stream(RecommendationCheck.values()).forEach(check -> checks.put(check, true));
        Arrays.stream(failed).forEach(check -> checks.put(check, false));
        return new CandidateVerdict(id, checks);
    }

    private static JudgeResult judge(CandidateVerdict... verdicts) {
        return new JudgeResult(List.of(verdicts), List.of());
    }

    private static List<String> ids(List<RecommendationCandidate> candidates) {
        return candidates.stream().map(RecommendationCandidate::id).toList();
    }
}
