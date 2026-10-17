package com.shoutoutz.api.feed.recommendation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.shoutoutz.api.common.llm.GeminiLlmClient;
import com.shoutoutz.api.common.llm.LlmProperties;
import com.shoutoutz.api.common.llm.OpenAiLlmClient;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationClaim;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRepository.GenerationTask;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationRetryPolicy;
import com.shoutoutz.api.feed.recommendation.application.QuestionRecommendationWorker;
import com.shoutoutz.api.feed.recommendation.application.dto.GeneratedQuestion;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.QuestionRecommendationPrompts;
import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationJudge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class LlmRecommendationFlowTest {

    private static final long FEED_ID = 42L;
    private static final int GENERATION = 3;
    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");
    private static final String GEMINI_URI =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
    private static final String OPENAI_URI = "https://api.openai.com/v1/responses";
    private static final String GEMINI_KEY = "gemini-private-api-key";
    private static final String OPENAI_KEY = "openai-private-api-key";
    private static final String PRIVATE_TITLE = "private-source-title";
    private static final String PRIVATE_CONTENT = "private-source-content";
    private static final String PRIVATE_ERROR = "provider-private-error-body";

    private final ObjectMapper mapper = new ObjectMapper();
    private final QuestionRecommendationRepository repository = mock(QuestionRecommendationRepository.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final Logger generatorLogger = (Logger) LoggerFactory.getLogger(LlmQuestionRecommendationGenerator.class);
    private final Logger workerLogger = (Logger) LoggerFactory.getLogger(QuestionRecommendationWorker.class);
    private final Logger judgeLogger = (Logger) LoggerFactory.getLogger(RecommendationJudge.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    };

    private MockRestServiceServer server;
    private QuestionRecommendationWorker worker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        GeminiLlmClient gemini = new GeminiLlmClient(
                builder.baseUrl("https://generativelanguage.googleapis.com").build(),
                new LlmProperties.Gemini(GEMINI_KEY, null, null, null), mapper);
        OpenAiLlmClient openai = new OpenAiLlmClient(
                builder.baseUrl("https://api.openai.com").build(),
                new LlmProperties.OpenAi(OPENAI_KEY, null, null, null, null), mapper);
        QuestionRecommendationProperties properties =
                new QuestionRecommendationProperties(5, 3, 5, 5000, true, null);
        QuestionRecommendationPrompts prompts = new QuestionRecommendationPrompts();
        LlmQuestionRecommendationGenerator generator = new LlmQuestionRecommendationGenerator(
                List.of(gemini, openai), properties, new QuestionRecommendationQualityProperties(8, 3, 3, null),
                prompts, new RecommendationJudge(List.of(gemini, openai), prompts, mapper), mapper);
        worker = new QuestionRecommendationWorker(repository, generator, properties,
                Clock.fixed(NOW, ZoneOffset.UTC), registry, new QuestionRecommendationRetryPolicy());
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(new GenerationClaim(GenerationClaim.ClaimStatus.CLAIMED,
                        new GenerationTask(FEED_ID, GENERATION, PRIVATE_TITLE, PRIVATE_CONTENT, 1)));

        logs.start();
        generatorLogger.addAppender(logs);
        workerLogger.addAppender(logs);
        judgeLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        generatorLogger.detachAppender(logs);
        workerLogger.detachAppender(logs);
        judgeLogger.detachAppender(logs);
        logs.stop();
        registry.close();
    }

    @Test
    void Gemini_503이면_OpenAI가_생성하고_장애난_Gemini_대신_OpenAI가_직접_검수해_저장한다() {
        expectGeminiFailure(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE", null);
        expectOpenAiText(RecommendationFixtures.generation(mapper, RecommendationFixtures.CANDIDATES));
        expectOpenAiText(RecommendationFixtures.judgement(8, List.of()));
        List<GeneratedQuestion> questions = IntStream.of(0, 1, 2, 4, 6)
                .mapToObj(RecommendationFixtures.CANDIDATES::get)
                .map(candidate -> new GeneratedQuestion(candidate.get("title"), candidate.get("content")))
                .toList();
        when(repository.completeGeneration(FEED_ID, GENERATION, questions, NOW)).thenReturn(true);

        var result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        assertThat(result.retryDelaySeconds()).isZero();
        verify(repository).completeGeneration(FEED_ID, GENERATION, questions, NOW);
        verify(repository, never()).retryGeneration(anyLong(), anyInt(), anyString(), any());
        verify(repository, never()).markGenerationFailed(anyLong(), anyInt(), anyString(), any());
        assertThat(registry.get("question.recommendation.generation.count").tag("outcome", "success")
                .counter().count()).isEqualTo(1);
        assertSafeCorrelatedLogs("provider=GEMINI, model=gemini-3.8-flash, httpStatus=503, "
                        + "errorCode=UNAVAILABLE, retryAfterSeconds=0, fallback=false",
                "provider=OPENAI, fallback=true, candidates=8",
                "질문 추천 judge 완료. provider=OPENAI, selfJudge=true",
                "provider=OPENAI, candidates=8, rulePassed=8, violations={}, hardGateFailures={}, "
                        + "duplicateGroups=0, selected=5");
        server.verify();
    }

    @Test
    void 부적합한_질문이면_검수없이_건너뜀으로_저장하고_확인한다() {
        expectGeminiText(RecommendationFixtures.skip(mapper, "OFF_TOPIC"));

        var result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        verify(repository).markGenerationSkipped(FEED_ID, GENERATION, "OFF_TOPIC", NOW);
        verify(repository, never()).completeGeneration(anyLong(), anyInt(), anyList(), any());
        assertThat(registry.get("question.recommendation.generation.count").tag("outcome", "skipped")
                .counter().count()).isEqualTo(1);
        assertSafeCorrelatedLogs("feedId=42, generation=3, reason=OFF_TOPIC");
        server.verify();
    }

    @Test
    void 교차_검수_제공자가_실패하면_생성한_Gemini가_직접_검수해_저장한다() {
        expectGeminiText(RecommendationFixtures.generation(mapper, RecommendationFixtures.CANDIDATES));
        expectOpenAiFailure(HttpStatus.SERVICE_UNAVAILABLE, "server_error");
        expectGeminiText(RecommendationFixtures.judgement(8, List.of()));
        when(repository.completeGeneration(anyLong(), anyInt(), anyList(), any())).thenReturn(true);

        var result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        verify(repository).completeGeneration(anyLong(), anyInt(), anyList(), any());
        assertSafeCorrelatedLogs("질문 추천 judge 호출 실패. provider=OPENAI, selfJudge=false, model=gpt-6-luna, "
                        + "httpStatus=503, errorCode=server_error",
                "질문 추천 judge 완료. provider=GEMINI, selfJudge=true");
        server.verify();
    }

    @Test
    void 두_검수_제공자가_모두_실패하면_저장없이_재시도한다() {
        expectGeminiText(RecommendationFixtures.generation(mapper, RecommendationFixtures.CANDIDATES));
        expectOpenAiFailure(HttpStatus.SERVICE_UNAVAILABLE, "server_error");
        expectGeminiFailure(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE", null);

        var result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isFalse();
        verify(repository).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
        verify(repository, never()).completeGeneration(anyLong(), anyInt(), anyList(), any());
        assertFailureMetric();
        assertSafeCorrelatedLogs("질문 추천 judge 호출 실패. provider=GEMINI, selfJudge=true");
        server.verify();
    }

    @Test
    void Gemini_429_RetryInfo와_OpenAI_503이면_저장없이_제공자의_대기시간으로_재시도한다() {
        expectGeminiFailure(HttpStatus.TOO_MANY_REQUESTS, "RESOURCE_EXHAUSTED", "120.1s");
        expectOpenAiFailure(HttpStatus.SERVICE_UNAVAILABLE, "server_error");

        var result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isFalse();
        assertThat(result.retryDelaySeconds()).isEqualTo(121);
        verify(repository).retryGeneration(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
        verify(repository, never()).completeGeneration(anyLong(), anyInt(), anyList(), any());
        verify(repository, never()).markGenerationFailed(anyLong(), anyInt(), anyString(), any());
        assertFailureMetric();
        assertSafeCorrelatedLogs("provider=GEMINI, model=gemini-3.8-flash, httpStatus=429, "
                        + "errorCode=RESOURCE_EXHAUSTED, retryAfterSeconds=121, fallback=false",
                "provider=OPENAI, model=gpt-6-luna, httpStatus=503, errorCode=server_error, "
                        + "retryAfterSeconds=0, fallback=true",
                "feedId=42, generation=3, attempts=1", "retryable=true, retryDelaySeconds=121");
        server.verify();
    }

    @Test
    void Gemini_인증실패와_OpenAI_결제한도_초과는_실패로_저장하고_재시도없이_확인한다() {
        expectGeminiFailure(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", null);
        expectOpenAiFailure(HttpStatus.TOO_MANY_REQUESTS, "insufficient_quota");

        var result = worker.process(FEED_ID, GENERATION);

        assertThat(result.acknowledged()).isTrue();
        assertThat(result.retryDelaySeconds()).isZero();
        verify(repository).markGenerationFailed(FEED_ID, GENERATION, "GENERATION_FAILED", NOW);
        verify(repository, never()).retryGeneration(anyLong(), anyInt(), anyString(), any());
        verify(repository, never()).completeGeneration(anyLong(), anyInt(), anyList(), any());
        assertFailureMetric();
        assertSafeCorrelatedLogs("provider=GEMINI, model=gemini-3.8-flash, httpStatus=401, "
                        + "errorCode=UNAUTHENTICATED, retryAfterSeconds=0, fallback=false",
                "provider=OPENAI, model=gpt-6-luna, httpStatus=429, errorCode=insufficient_quota, "
                        + "retryAfterSeconds=0, fallback=true",
                "retryable=false, retryDelaySeconds=0");
        server.verify();
    }

    private void expectGeminiFailure(HttpStatus status, String code, String retryDelay) {
        Map<String, Object> error = retryDelay == null
                ? Map.of("status", code, "message", PRIVATE_ERROR)
                : Map.of("status", code, "message", PRIVATE_ERROR, "details", List.of(Map.of(
                        "@type", "type.googleapis.com/google.rpc.RetryInfo", "retryDelay", retryDelay)));
        server.expect(requestTo(GEMINI_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", GEMINI_KEY))
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(Map.of("error", error))));
    }

    private void expectGeminiText(String text) {
        server.expect(requestTo(GEMINI_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", GEMINI_KEY))
                .andRespond(withSuccess(mapper.writeValueAsString(Map.of("candidates", List.of(Map.of(
                        "finishReason", "STOP", "content", Map.of("parts", List.of(Map.of("text", text))))))),
                        MediaType.APPLICATION_JSON));
    }

    private void expectOpenAiText(String text) {
        server.expect(requestTo(OPENAI_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + OPENAI_KEY))
                .andRespond(withSuccess(mapper.writeValueAsString(Map.of("status", "completed", "output", List.of(
                        Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", text)))))),
                        MediaType.APPLICATION_JSON));
    }

    private void expectOpenAiFailure(HttpStatus status, String code) {
        server.expect(requestTo(OPENAI_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + OPENAI_KEY))
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(Map.of("error", Map.of("code", code, "message", PRIVATE_ERROR)))));
    }

    private void assertSafeCorrelatedLogs(String... expectedMessages) {
        String messages = logs.list.stream().map(ILoggingEvent::getFormattedMessage)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(messages).contains(expectedMessages)
                .doesNotContain(GEMINI_KEY, OPENAI_KEY, PRIVATE_TITLE, PRIVATE_CONTENT, PRIVATE_ERROR);
        assertThat(logs.list).isNotEmpty().allSatisfy(event -> {
            assertThat(event.getMDCPropertyMap()).containsEntry("traceId", "recommendation-42-3-1");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    private void assertFailureMetric() {
        assertThat(registry.get("question.recommendation.generation.count").tag("outcome", "failure")
                .counter().count()).isEqualTo(1);
    }
}
