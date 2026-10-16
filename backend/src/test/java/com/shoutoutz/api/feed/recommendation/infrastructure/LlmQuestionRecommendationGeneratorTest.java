package com.shoutoutz.api.feed.recommendation.infrastructure;

import static com.shoutoutz.api.feed.recommendation.infrastructure.RecommendationFixtures.CANDIDATES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.llm.LlmClient;
import com.shoutoutz.api.common.llm.LlmException;
import com.shoutoutz.api.common.llm.LlmProvider;
import com.shoutoutz.api.common.llm.LlmRequest;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationGenerationException;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationSkippedException;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.CandidateVerdict;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.JudgeResult;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.QuestionRecommendationPrompts;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationCandidate;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationCheck;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationJudge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class LlmQuestionRecommendationGeneratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VALID = RecommendationFixtures.generation(MAPPER, CANDIDATES);

    private final LlmClient gemini = mock(LlmClient.class);
    private final LlmClient openai = mock(LlmClient.class);
    private final RecommendationJudge judge = mock(RecommendationJudge.class);
    private LlmQuestionRecommendationGenerator generator;

    @BeforeEach
    void setUp() {
        when(gemini.provider()).thenReturn(LlmProvider.GEMINI);
        when(openai.provider()).thenReturn(LlmProvider.OPENAI);
        when(judge.judge(any(), anyString(), anyList())).thenAnswer(invocation -> passAll(invocation.getArgument(2)));
        generator = generator(true);
    }

    @Test
    void Gemini가_만든_후보를_OpenAI_judge로_검수해_각도가_다양한_5개를_고른다() {
        when(gemini.generate(any())).thenReturn(VALID);

        List<GeneratedQuestion> questions = generator.generate("원 제목", "원 본문", 5);

        assertThat(questions).extracting(GeneratedQuestion::title).containsExactly(
                CANDIDATES.get(0).get("title"), CANDIDATES.get(1).get("title"), CANDIDATES.get(2).get("title"),
                CANDIDATES.get(4).get("title"), CANDIDATES.get(6).get("title"));
        verify(openai, never()).generate(any());
        verify(judge).judge(eq(LlmProvider.GEMINI), eq("{\"title\":\"원 제목\",\"content\":\"원 본문\"}"), anyList());
        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(gemini).generate(request.capture());
        assertThat(request.getValue().prompt()).contains("후보 8개", "원 제목", "원 본문");
        assertThat(request.getValue().maxOutputTokens()).isEqualTo(8_192);
        Map<?, ?> questionsSchema = (Map<?, ?>) ((Map<?, ?>) request.getValue().responseSchema().get("properties"))
                .get("questions");
        assertThat(questionsSchema.get("maxItems")).isEqualTo(8);
    }

    @Test
    void Gemini_HTTP_실패시_같은_요청을_OpenAI로_생성하고_Gemini가_검수한다() {
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 429, true, 60));
        when(openai.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("제목", "본문", 5)).hasSize(5);

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(gemini).generate(request.capture());
        verify(openai).generate(request.getValue());
        verify(judge).judge(eq(LlmProvider.OPENAI), anyString(), anyList());
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void 잘못된_Gemini_결과는_OpenAI로_대체한다(String invalid) {
        when(gemini.generate(any())).thenReturn(invalid);
        when(openai.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("제목", "본문", 5)).hasSize(5);
        verify(openai).generate(any());
    }

    static Stream<String> invalidResponses() {
        List<Map<String, String>> twoAngles = CANDIDATES.stream()
                .map(candidate -> RecommendationFixtures.candidate(
                        candidate.get("angle").equals("COMPARE") || candidate.get("angle").equals("PRACTICE")
                                ? "DEEPEN" : candidate.get("angle"),
                        candidate.get("title"), candidate.get("content")))
                .toList();
        List<Map<String, String>> unknownAngle = new ArrayList<>(CANDIDATES);
        unknownAngle.set(0, RecommendationFixtures.candidate("UNKNOWN", "제목", "본문"));
        List<Map<String, String>> blankTitle = new ArrayList<>(CANDIDATES);
        blankTitle.set(0, RecommendationFixtures.candidate("DEEPEN", " ", "본문"));
        return Stream.of("", "not json", "null", "{}", "{\"decision\":\"MAYBE\",\"questions\":[]}",
                RecommendationFixtures.skip(MAPPER, "NONE"),
                RecommendationFixtures.generation(MAPPER, CANDIDATES.subList(0, 2)),
                RecommendationFixtures.generation(MAPPER, twoAngles),
                RecommendationFixtures.generation(MAPPER, unknownAngle),
                RecommendationFixtures.generation(MAPPER, blankTitle));
    }

    @Test
    void 부적합한_원글이면_대체와_검수없이_건너뛴다() {
        when(gemini.generate(any())).thenReturn(RecommendationFixtures.skip(MAPPER, "TOO_VAGUE"));

        assertThatThrownBy(() -> generator.generate("ㅎㅇ", "ㅎㅇ", 5))
                .isInstanceOfSatisfying(QuestionRecommendationSkippedException.class,
                        exception -> assertThat(exception.reason()).isEqualTo("TOO_VAGUE"));
        verify(openai, never()).generate(any());
        verify(judge, never()).judge(any(), anyString(), anyList());
    }

    @Test
    void 완전히_같은_후보는_하나만_남긴다() {
        List<Map<String, String>> duplicated = new ArrayList<>(CANDIDATES);
        duplicated.set(7, CANDIDATES.get(6));
        when(gemini.generate(any())).thenReturn(RecommendationFixtures.generation(MAPPER, duplicated));

        generator.generate("제목", "본문", 5);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RecommendationCandidate>> candidates = ArgumentCaptor.forClass(List.class);
        verify(judge).judge(any(), anyString(), candidates.capture());
        assertThat(candidates.getValue()).hasSize(7);
    }

    @Test
    void 규칙_검증_통과_후보가_최소_개수보다_적으면_검수없이_재시도한다() {
        List<Map<String, String>> mostlyShort = new ArrayList<>(CANDIDATES.stream()
                .map(candidate -> RecommendationFixtures.candidate(candidate.get("angle"), candidate.get("title"), "짧은 본문"))
                .toList());
        mostlyShort.set(0, CANDIDATES.get(0));
        mostlyShort.set(2, CANDIDATES.get(2));
        when(gemini.generate(any())).thenReturn(RecommendationFixtures.generation(MAPPER, mostlyShort));

        assertThatThrownBy(() -> generator.generate("제목", "본문", 5))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryable()).isTrue());
        verify(judge, never()).judge(any(), anyString(), anyList());
    }

    @Test
    void 검수를_통과한_후보가_최소_개수보다_적으면_재시도한다() {
        when(gemini.generate(any())).thenReturn(VALID);
        when(judge.judge(any(), anyString(), anyList())).thenAnswer(invocation -> {
            List<RecommendationCandidate> candidates = invocation.getArgument(2);
            return new JudgeResult(candidates.stream()
                    .map(candidate -> verdict(candidate.id(), candidate.order() > 2))
                    .toList(), List.of());
        });

        assertThatThrownBy(() -> generator.generate("제목", "본문", 5))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryable()).isTrue());
    }

    @Test
    void 검수_실패는_그대로_전달하고_다른_제공자로_다시_생성하지_않는다() {
        when(gemini.generate(any())).thenReturn(VALID);
        when(judge.judge(any(), anyString(), anyList())).thenThrow(new QuestionRecommendationGenerationException(true, 30));

        assertThatThrownBy(() -> generator.generate("제목", "본문", 5))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryAfterSeconds()).isEqualTo(30));
        verify(openai, never()).generate(any());
    }

    @Test
    void 모든_제공자가_영구_실패하면_재시도하지_않는다() {
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 401, false, 0));
        when(openai.generate(any())).thenThrow(failure(LlmProvider.OPENAI, 403, false, 0));

        assertThatThrownBy(() -> generator.generate("제목", "본문", 5))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class, exception -> {
                    assertThat(exception.retryable()).isFalse();
                    assertThat(exception.retryAfterSeconds()).isZero();
                    assertThat(exception.getCause()).isNull();
                });
    }

    @Test
    void 재시도_가능한_실패가_있으면_대기시간을_전달한다() {
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 429, true, 60));
        when(openai.generate(any())).thenThrow(failure(LlmProvider.OPENAI, 503, true, 10));

        assertThatThrownBy(() -> generator.generate("제목", "본문", 5))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.retryAfterSeconds()).isEqualTo(60);
                });
    }

    @Test
    void 자동대체가_꺼지면_Gemini_실패를_그대로_전달한다() {
        generator = generator(false);
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 429, true, 30));

        assertThatThrownBy(() -> generator.generate("제목", "본문", 5))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryAfterSeconds()).isEqualTo(30));
        verify(openai, never()).generate(any());
    }

    private LlmQuestionRecommendationGenerator generator(boolean fallbackEnabled) {
        return new LlmQuestionRecommendationGenerator(List.of(gemini, openai),
                new QuestionRecommendationProperties(5, 5, 5, 5000, fallbackEnabled, null),
                new QuestionRecommendationQualityProperties(8, 3, 3, null),
                new QuestionRecommendationPrompts(), judge, MAPPER);
    }

    private static JudgeResult passAll(List<RecommendationCandidate> candidates) {
        return new JudgeResult(candidates.stream().map(candidate -> verdict(candidate.id(), false)).toList(), List.of());
    }

    private static CandidateVerdict verdict(String id, boolean failHardGate) {
        Map<RecommendationCheck, Boolean> checks = new EnumMap<>(RecommendationCheck.class);
        Arrays.stream(RecommendationCheck.values()).forEach(check -> checks.put(check, true));
        checks.put(RecommendationCheck.SAFE, !failHardGate);
        return new CandidateVerdict(id, checks);
    }

    private LlmException failure(LlmProvider provider, int status, boolean retryable, long delay) {
        return new LlmException(provider, "model", status, "UPSTREAM_ERROR", retryable, delay);
    }
}
