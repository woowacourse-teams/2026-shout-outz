package com.shoutoutz.api.feed.recommendation.infrastructure;

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
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class LlmQuestionRecommendationGeneratorTest {

    private static final String VALID = """
            {"questions":[{"title":" 추천 제목 ","content":" 추천 본문 "}]}
            """;

    private final LlmClient gemini = mock(LlmClient.class);
    private final LlmClient openai = mock(LlmClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private LlmQuestionRecommendationGenerator generator;

    @BeforeEach
    void setUp() {
        when(gemini.provider()).thenReturn(LlmProvider.GEMINI);
        when(openai.provider()).thenReturn(LlmProvider.OPENAI);
        generator = generator(true);
    }

    @Test
    void Gemini_성공시_OpenAI를_호출하지_않고_정규화한다() {
        when(gemini.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("원 제목", "원 본문", 1))
                .containsExactly(new GeneratedQuestion("추천 제목", "추천 본문"));
        verify(openai, never()).generate(any());
        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(gemini).generate(request.capture());
        assertThat(request.getValue().prompt()).contains("정확히 1개", "원 제목", "원 본문", "200~600자");
        Map<?, ?> questions = (Map<?, ?>) ((Map<?, ?>) request.getValue().responseSchema().get("properties"))
                .get("questions");
        assertThat(questions.get("minItems")).isEqualTo(1);
        assertThat(questions.get("maxItems")).isEqualTo(1);
    }

    @Test
    void Gemini_HTTP_실패시_동일한_요청을_OpenAI로_대체한다() {
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 429, true, 60));
        when(openai.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("제목", "본문", 1)).hasSize(1);

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(gemini).generate(request.capture());
        verify(openai).generate(request.getValue());
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void 잘못된_Gemini_결과도_OpenAI로_대체한다(String invalid) {
        when(gemini.generate(any())).thenReturn(invalid);
        when(openai.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("제목", "본문", 1)).hasSize(1);
        verify(openai).generate(any());
    }

    static List<String> invalidResponses() {
        return List.of("", "not json", "null", "{}", "{\"questions\":[]}",
                "{\"questions\":[null]}", "{\"questions\":[{\"title\":\"제목\"}]}",
                "{\"questions\":[{\"title\":\" \",\"content\":\"본문\"}]}",
                "{\"questions\":[{\"title\":\"" + "가".repeat(101) + "\",\"content\":\"본문\"}]}",
                "{\"questions\":[{\"title\":\"제목\",\"content\":\"" + "가".repeat(5001) + "\"}]}");
    }

    @Test
    void 중복된_추천_결과는_대체한다() {
        String duplicates = "{\"questions\":[{\"title\":\"A\",\"content\":\"B\"},"
                + "{\"title\":\" a \",\"content\":\" b \"}]}";
        when(gemini.generate(any())).thenReturn(duplicates);
        when(openai.generate(any())).thenReturn("""
                {"questions":[{"title":"A","content":"B"},{"title":"C","content":"D"}]}
                """);

        assertThat(generator.generate("제목", "본문", 2)).hasSize(2);
        verify(openai).generate(any());
    }

    @Test
    void 제외할_제목을_프롬프트에_포함한다() {
        when(gemini.generate(any())).thenReturn(VALID);

        generator.generate("원 제목", "원 본문", 1, List.of("기존 추천", "사용된 추천"));

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(gemini).generate(request.capture());
        assertThat(request.getValue().prompt())
                .contains("의미가 겹치지 않는 새 질문", "[\"기존 추천\",\"사용된 추천\"]");
    }

    @Test
    void 제외할_제목이_없으면_제외_지시를_넣지_않는다() {
        when(gemini.generate(any())).thenReturn(VALID);

        generator.generate("원 제목", "원 본문", 1, List.of());

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(gemini).generate(request.capture());
        assertThat(request.getValue().prompt()).doesNotContain("의미가 겹치지 않는 새 질문");
    }

    @Test
    void 제외할_제목과_같은_추천_결과는_대체한다() {
        when(gemini.generate(any())).thenReturn("""
                {"questions":[{"title":" 기존 추천 ","content":"본문"}]}
                """);
        when(openai.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("제목", "본문", 1, List.of(" 기존 추천")))
                .containsExactly(new GeneratedQuestion("추천 제목", "추천 본문"));
        verify(openai).generate(any());
    }

    @Test
    void 제외할_제목과_대소문자만_다른_추천_결과도_거부한다() {
        String excluded = "{\"questions\":[{\"title\":\"Java Stream\",\"content\":\"본문\"}]}";
        when(gemini.generate(any())).thenReturn(excluded);
        when(openai.generate(any())).thenReturn(excluded);

        assertThatThrownBy(() -> generator.generate("제목", "본문", 1, List.of("java stream ")))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryable()).isTrue());
    }

    @Test
    void 대체_추천은_한_개만_생성한다() {
        when(gemini.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("제목", "본문", 1, List.of("기존 추천")))
                .containsExactly(new GeneratedQuestion("추천 제목", "추천 본문"));
        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(gemini).generate(request.capture());
        assertThat(request.getValue().prompt()).contains("정확히 1개");
        Map<?, ?> questions = (Map<?, ?>) ((Map<?, ?>) request.getValue().responseSchema().get("properties"))
                .get("questions");
        assertThat(questions.get("minItems")).isEqualTo(1);
        assertThat(questions.get("maxItems")).isEqualTo(1);
    }

    @Test
    void 모든_제공자가_영구_실패하면_재시도하지_않는다() {
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 401, false, 0));
        when(openai.generate(any())).thenThrow(failure(LlmProvider.OPENAI, 403, false, 0));

        assertThatThrownBy(() -> generator.generate("제목", "본문", 1))
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

        assertThatThrownBy(() -> generator.generate("제목", "본문", 1))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class, exception -> {
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.retryAfterSeconds()).isEqualTo(60);
                });
    }

    @Test
    void OpenAI의_결과도_동일하게_검증한다() {
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 401, false, 0));
        when(openai.generate(any())).thenReturn("{\"questions\":[]}");

        assertThatThrownBy(() -> generator.generate("제목", "본문", 1))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryable()).isTrue());
    }

    @Test
    void 자동대체가_꺼지면_Gemini_실패를_그대로_전달한다() {
        generator = generator(false);
        when(gemini.generate(any())).thenThrow(failure(LlmProvider.GEMINI, 429, true, 30));

        assertThatThrownBy(() -> generator.generate("제목", "본문", 1))
                .isInstanceOfSatisfying(QuestionRecommendationGenerationException.class,
                        exception -> assertThat(exception.retryAfterSeconds()).isEqualTo(30));
        verify(openai, never()).generate(any());
    }

    @Test
    void 설정한_순서대로_제공자를_호출한다() {
        generator = new LlmQuestionRecommendationGenerator(List.of(gemini, openai),
                new QuestionRecommendationProperties(5, 5, 5, 5000, true, null,
                        List.of(LlmProvider.OPENAI, LlmProvider.GEMINI)), objectMapper);
        when(openai.generate(any())).thenReturn(VALID);

        assertThat(generator.generate("제목", "본문", 1)).hasSize(1);
        verify(gemini, never()).generate(any());
    }

    @Test
    void 설정한_제공자의_클라이언트가_없으면_생성기를_만들_수_없다() {
        assertThatThrownBy(() -> new LlmQuestionRecommendationGenerator(List.of(gemini, openai),
                new QuestionRecommendationProperties(5, 5, 5, 5000, true, null, null), objectMapper))
                .hasMessageContaining("BEDROCK_GPT");
    }

    private LlmQuestionRecommendationGenerator generator(boolean fallbackEnabled) {
        return new LlmQuestionRecommendationGenerator(List.of(gemini, openai),
                new QuestionRecommendationProperties(5, 5, 5, 5000, fallbackEnabled, null,
                        List.of(LlmProvider.GEMINI, LlmProvider.OPENAI)), objectMapper);
    }

    private LlmException failure(LlmProvider provider, int status, boolean retryable, long delay) {
        return new LlmException(provider, "model", status, "UPSTREAM_ERROR", retryable, delay);
    }
}
