package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.feed.recommendation.infrastructure.QuestionRecommendationQualityProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecommendationRuleValidatorTest {

    private static final String SOURCE_TITLE = "JPA N+1 문제는 왜 발생하나요?";
    private static final String TITLE = "트랜잭션 전파 속성은 언제 바꿔야 하나요?";
    private static final String CONTENT = "트랜잭션 전파 속성을 새 트랜잭션으로 설정했을 때 외부 트랜잭션이 롤백되면 "
            + "내부에서 커밋한 데이터는 어떻게 남는지 궁금합니다. 실무에서는 이런 상황을 어떻게 설계하고 "
            + "운영하는지 경험을 듣고 싶습니다.";

    private final RecommendationRuleValidator validator =
            new RecommendationRuleValidator(new QuestionRecommendationQualityProperties.Rules(10, 60, 80, 700, 0.4, 0.6, 0.5));

    @Test
    void 규칙을_모두_지킨_한국어_후보는_통과한다() {
        RuleValidationResult result = validate(candidate("c1", TITLE, CONTENT));

        assertThat(result.passed()).extracting(RecommendationCandidate::id).containsExactly("c1");
        assertThat(result.rejected()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"10, true", "9, false", "60, true", "61, false"})
    void 제목_길이_경계를_검사한다(int length, boolean passes) {
        assertPassesOrViolates(candidate("c1", "가".repeat(length), CONTENT), passes, RuleViolation.LENGTH);
    }

    @ParameterizedTest
    @CsvSource({"80, true", "79, false", "700, true", "701, false"})
    void 본문_길이_경계를_검사한다(int length, boolean passes) {
        assertPassesOrViolates(candidate("c1", TITLE, "나".repeat(length)), passes, RuleViolation.LENGTH);
    }

    @Test
    void 길이는_앞뒤_공백을_제외하고_센다() {
        RuleValidationResult result = validate(candidate("c1", "   " + "가".repeat(9) + "   ", CONTENT));

        assertThat(result.rejected()).containsEntry("c1", RuleViolation.LENGTH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"위 질문", "위의 질문", "위 글", "원글", "앞서 말", "작성자님", "질문자님"})
    void 원글을_가리키는_표현은_거부한다(String phrase) {
        RuleValidationResult result = validate(candidate("c1", TITLE, phrase + "에서 다룬 내용과 관련해서 " + CONTENT));

        assertThat(result.rejected()).containsEntry("c1", RuleViolation.SOURCE_REFERENCE);
    }

    @Test
    void 제목에_있는_원글_지칭도_거부한다() {
        RuleValidationResult result = validate(candidate("c1", "작성자님은 트랜잭션을 어떻게 쓰나요?", CONTENT));

        assertThat(result.rejected()).containsEntry("c1", RuleViolation.SOURCE_REFERENCE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "참고 링크 https://example.com 를 보면",
            "참고 링크 http://example.com 를 보면",
            "참고 링크 www.example.com 를 보면",
            "문의는 someone@example.com 으로 주세요",
            "## 배경\n",
            "AI로서 말씀드리면",
            "언어 모델로서 보면",
            "AI 어시스턴트가 보기에",
            "추천 질문입니다"
    })
    void 금지_토큰이_있으면_거부한다(String token) {
        RuleValidationResult result = validate(candidate("c1", TITLE, token + " " + CONTENT));

        assertThat(result.rejected()).containsEntry("c1", RuleViolation.FORBIDDEN_TOKEN);
    }

    @Test
    void 줄_중간의_샵은_마크다운_제목으로_보지_않는다() {
        RuleValidationResult result = validate(candidate("c1", TITLE, CONTENT + " C# 언어에서도 같은 문제가 있나요?"));

        assertThat(result.passed()).hasSize(1);
    }

    @Test
    void 한국어_비율이_낮으면_거부한다() {
        String english = "How should we design transaction propagation when the outer transaction rolls back "
                + "and the inner one already committed its changes?";
        RuleValidationResult result = validate(candidate("c1", "Transaction propagation design", english));

        assertThat(result.rejected()).containsEntry("c1", RuleViolation.NOT_KOREAN);
    }

    @Test
    void 글자가_하나도_없으면_한국어가_아닌_것으로_본다() {
        RuleValidationResult result = validate(candidate("c1", "1234567890", "1".repeat(80)));

        assertThat(result.rejected()).containsEntry("c1", RuleViolation.NOT_KOREAN);
    }

    @Test
    void 코드_블록과_인라인_코드는_한국어_비율에서_제외한다() {
        String content = "아래 코드에서 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 설정이 동작하는 방식이 궁금합니다.\n"
                + "```java\n"
                + "@Transactional(propagation = Propagation.REQUIRES_NEW)\n"
                + "public void saveHistory(OrderHistory history) { historyRepository.save(history); }\n"
                + "```\n"
                + "외부 롤백 이후에도 기록이 남는 이유를 알고 싶습니다.";

        RuleValidationResult result = validate(candidate("c1", TITLE, content));

        assertThat(result.passed()).hasSize(1);
    }

    @Test
    void 원글_제목과_너무_비슷하면_거부한다() {
        RuleValidationResult result = validate(candidate("c1", "JPA N+1 문제는 왜 발생할까요?", CONTENT));

        assertThat(result.rejected()).containsEntry("c1", RuleViolation.SOURCE_SIMILAR);
    }

    @Test
    void 여러_규칙을_어기면_먼저_검사한_위반만_기록한다() {
        RuleValidationResult result = validate(
                candidate("c1", "원글 질문", CONTENT),
                candidate("c2", TITLE, "원글 https://example.com " + CONTENT),
                candidate("c3", "Transaction propagation", "see https://example.com " + "a".repeat(80)),
                candidate("c4", SOURCE_TITLE, "a".repeat(80)));

        assertThat(result.rejected()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "c1", RuleViolation.LENGTH,
                "c2", RuleViolation.SOURCE_REFERENCE,
                "c3", RuleViolation.FORBIDDEN_TOKEN,
                "c4", RuleViolation.NOT_KOREAN));
    }

    @Test
    void 통과한_후보는_입력_순서를_유지한다() {
        RuleValidationResult result = validate(
                candidate("c3", TITLE, CONTENT),
                candidate("c1", "짧은 제목", CONTENT),
                candidate("c2", "스프링 이벤트는 언제 쓰면 좋을까요?", CONTENT),
                candidate("c4", "테스트 격리는 어떻게 하시나요?", CONTENT));

        assertThat(result.passed()).extracting(RecommendationCandidate::id).containsExactly("c3", "c2", "c4");
        assertThat(result.rejected()).containsOnlyKeys("c1");
    }

    private RuleValidationResult validate(RecommendationCandidate... candidates) {
        return validator.validate(SOURCE_TITLE, List.of(candidates));
    }

    private void assertPassesOrViolates(RecommendationCandidate candidate, boolean passes, RuleViolation violation) {
        RuleValidationResult result = validate(candidate);
        if (passes) {
            assertThat(result.passed()).containsExactly(candidate);
            assertThat(result.rejected()).isEmpty();
        } else {
            assertThat(result.passed()).isEmpty();
            assertThat(result.rejected()).containsEntry(candidate.id(), violation);
        }
    }

    private static RecommendationCandidate candidate(String id, String title, String content) {
        return new RecommendationCandidate(id, Integer.parseInt(id.substring(1)), RecommendationAngle.DEEPEN, title, content);
    }
}
