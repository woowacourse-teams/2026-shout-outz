package com.shoutoutz.api.feed.recommendation.infrastructure.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.llm.LlmClient;
import com.shoutoutz.api.common.llm.LlmException;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.common.llm.LlmRequest;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerationException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class RecommendationJudgeTest {

    private static final String SOURCE = "{\"title\":\"원 제목\",\"content\":\"원 본문\"}";
    private static final List<RecommendationCandidate> CANDIDATES = List.of(
            new RecommendationCandidate("c1", 1, RecommendationAngle.DEEPEN, "제목1", "본문1"),
            new RecommendationCandidate("c2", 2, RecommendationAngle.COMPARE, "제목2", "본문2"));

    private final LlmClient gemini = mock(LlmClient.class);
    private final LlmClient openai = mock(LlmClient.class);
    private RecommendationJudge judge;

    @BeforeEach
    void setUp() {
        when(gemini.provider()).thenReturn(LlmProvider.GEMINI);
        when(openai.provider()).thenReturn(LlmProvider.OPENAI);
        judge = new RecommendationJudge(List.of(gemini, openai), new QuestionRecommendationPrompts(),
                new ObjectMapper(), new Random(0));
    }

    @Test
    void 생성과_다른_제공자가_temperature_0으로_채점한다() {
        when(openai.generate(any())).thenReturn(response(result("c1", "SAFE"), result("c2"), "[[\"c1\",\"c2\"]]"));

        JudgeResult result = judge.judge(LlmProvider.GEMINI, SOURCE, CANDIDATES);

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(openai).generate(request.capture());
        verify(gemini, never()).generate(any());
        assertThat(request.getValue().temperature()).isEqualTo(0.0);
        assertThat(request.getValue().prompt()).contains("원 제목", "\"id\":\"c1\"", "\"id\":\"c2\"");
        assertThat(result.verdicts()).extracting(CandidateVerdict::candidateId).containsExactly("c1", "c2");
        assertThat(result.verdicts().get(0).passesHardGates()).isFalse();
        assertThat(result.verdicts().get(1).passesHardGates()).isTrue();
        assertThat(result.duplicateGroups()).containsExactly(List.of("c1", "c2"));
    }

    @Test
    void OpenAI가_생성했으면_Gemini가_채점한다() {
        when(gemini.generate(any())).thenReturn(response(result("c1"), result("c2"), "[]"));

        judge.judge(LlmProvider.OPENAI, SOURCE, CANDIDATES);

        verify(openai, never()).generate(any());
    }

    @Test
    void 판정_스키마는_근거를_판정보다_먼저_요구한다() {
        when(openai.generate(any())).thenReturn(response(result("c1"), result("c2"), "[]"));

        judge.judge(LlmProvider.GEMINI, SOURCE, CANDIDATES);

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(openai).generate(request.capture());
        Map<?, ?> results = (Map<?, ?>) ((Map<?, ?>) request.getValue().responseSchema().get("properties")).get("results");
        Map<?, ?> checks = (Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) results.get("items")).get("properties")).get("checks");
        Map<?, ?> checkProperties = (Map<?, ?>) ((Map<?, ?>) checks.get("items")).get("properties");
        assertThat(checkProperties.keySet().stream().map(String::valueOf).toList())
                .containsExactly("item", "why", "pass");
    }

    @Test
    void 중복_묶음의_모르는_id와_한개짜리_묶음은_버린다() {
        when(openai.generate(any())).thenReturn(response(result("c1"), result("c2"), "[[\"c1\",\"c9\"],[\"c2\",\"c2\"]]"));

        assertThat(judge.judge(LlmProvider.GEMINI, SOURCE, CANDIDATES).duplicateGroups()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void 잘못된_판정은_재시도한다(String invalid) {
        when(openai.generate(any())).thenReturn(invalid);

        assertThatThrownBy(() -> judge.judge(LlmProvider.GEMINI, SOURCE, CANDIDATES))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryable()).isTrue());
    }

    static Stream<String> invalidResponses() {
        String c1 = result("c1");
        return Stream.of(
                "not json",
                "{}",
                response(c1, "", "[]").replace(",]", "]"),
                response(c1, result("c3"), "[]"),
                response(c1, c1, "[]"),
                response(c1, result("c2").replace("{\"item\":\"SAFE\",\"why\":\"근거\",\"pass\":true},", ""), "[]"),
                response(c1, result("c2").replace("\"pass\":true}]", "\"pass\":\"yes\"}]"), "[]"),
                response(c1, result("c2").replace("\"item\":\"SAFE\"", "\"item\":\"UNKNOWN\""), "[]"));
    }

    @Test
    void judge_호출_실패는_영구_실패여도_재시도한다() {
        when(openai.generate(any())).thenThrow(new LlmException(LlmProvider.OPENAI, "model", 401, "UNAUTHORIZED",
                false, 0));

        assertThatThrownBy(() -> judge.judge(LlmProvider.GEMINI, SOURCE, CANDIDATES))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryable()).isTrue());
    }

    @Test
    void 제공자_대기시간을_전달한다() {
        when(openai.generate(any())).thenThrow(new LlmException(LlmProvider.OPENAI, "model", 429, "RATE", true, 40));

        assertThatThrownBy(() -> judge.judge(LlmProvider.GEMINI, SOURCE, CANDIDATES))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryAfterSeconds()).isEqualTo(40));
    }

    private static String result(String id, String... failedChecks) {
        List<String> failed = List.of(failedChecks);
        String checks = Arrays.stream(RecommendationCheck.values())
                .map(check -> "{\"item\":\"" + check.name() + "\",\"why\":\"근거\",\"pass\":"
                        + !failed.contains(check.name()) + "}")
                .collect(Collectors.joining(","));
        return "{\"id\":\"" + id + "\",\"checks\":[" + checks + "]}";
    }

    private static String response(String first, String second, String duplicateGroups) {
        return "{\"results\":[" + first + "," + second + "],\"duplicateGroups\":" + duplicateGroups + "}";
    }
}
