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
        LlmQuestionRecommendationGenerator generator =
                new LlmQuestionRecommendationGenerator(List.of(gemini, openai), properties, mapper);
        worker = new QuestionRecommendationWorker(repository, generator, properties,
                Clock.fixed(NOW, ZoneOffset.UTC), registry, new QuestionRecommendationRetryPolicy());
        when(repository.claimGeneration(FEED_ID, GENERATION, NOW, NOW.plusSeconds(180)))
                .thenReturn(new GenerationClaim(GenerationClaim.ClaimStatus.CLAIMED,
                        new GenerationTask(FEED_ID, GENERATION, PRIVATE_TITLE, PRIVATE_CONTENT, 1)));

        logs.start();
        generatorLogger.addAppender(logs);
        workerLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        generatorLogger.detachAppender(logs);
        workerLogger.detachAppender(logs);
        logs.stop();
        registry.close();
    }

    @Test
    void Gemini_503이면_OpenAI_응답_다섯개를_검증하고_저장한뒤_확인한다() {
        expectGeminiFailure(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE", null);
        List<GeneratedQuestion> questions = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> new GeneratedQuestion("추천 제목 " + index, "추천 본문 " + index))
                .toList();
        String generated = mapper.writeValueAsString(Map.of("questions", questions));
        String response = mapper.writeValueAsString(Map.of("status", "completed", "output", List.of(
                Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text", generated))))));
        server.expect(requestTo(OPENAI_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + OPENAI_KEY))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
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
                "provider=OPENAI, fallback=true");
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
