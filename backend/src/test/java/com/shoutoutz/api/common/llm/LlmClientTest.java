package com.shoutoutz.api.common.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class LlmClientTest {
    private static final String GEMINI_URI = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent";
    private static final String OPENAI_URI = "https://api.openai.com/v1/responses";
    private static final String SECRET = "test-secret-key";
    private static final LlmRequest REQUEST = new LlmRequest("private prompt", Map.of("type", "object"), 4096);
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private MockRestServiceServer server;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void geminiSendsKeyOnlyInHeaderAndJsonSchemaAndCombinesNonThoughtParts() {
        server.expect(requestTo(GEMINI_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", SECRET))
                .andExpect(content().json("""
                        {"contents":[{"role":"user","parts":[{"text":"private prompt"}]}],
                        "generationConfig":{"responseMimeType":"application/json","responseJsonSchema":{"type":"object"},
                        "maxOutputTokens":4096}}
                        """))
                .andRespond(withSuccess("""
                        {"candidates":[{"finishReason":"STOP","content":{"parts":[
                        {"text":"hidden reasoning","thought":true},{"text":"{\\"questions\\":"},{"text":"[]}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(gemini().generate(REQUEST)).isEqualTo("{\"questions\":[]}");
        server.verify();
    }

    @Test
    void geminiSendsTemperatureOnlyWhenRequested() {
        server.expect(requestTo(GEMINI_URI))
                .andExpect(content().json("""
                        {"generationConfig":{"temperature":0.0}}
                        """))
                .andRespond(withSuccess("""
                        {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"{}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        gemini().generate(new LlmRequest("private prompt", Map.of("type", "object"), 4096, 0.0));
        server.verify();
    }

    @Test
    void openAiIgnoresTemperatureForReasoningModels() {
        server.expect(requestTo(OPENAI_URI))
                .andExpect(request -> assertThat(request.getBody().toString()).doesNotContain("temperature"))
                .andRespond(withSuccess("""
                        {"status":"completed","output":[{"type":"message","content":[
                        {"type":"output_text","text":"{}"}]}]}
                        """, MediaType.APPLICATION_JSON));

        openai().generate(new LlmRequest("private prompt", Map.of("type", "object"), 4096, 0.0));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 2.1})
    void temperatureOutOfRangeIsRejected(double temperature) {
        assertThat(catchThrowableOfType(IllegalArgumentException.class,
                () -> new LlmRequest("prompt", Map.of(), 1, temperature))).isNotNull();
    }

    @Test
    void openAiUsesResponsesWithStrictSchemaAndDoesNotStoreRequest() {
        server.expect(requestTo(OPENAI_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + SECRET))
                .andExpect(content().json("""
                        {"model":"gpt-6-luna","input":"private prompt","store":false,"max_output_tokens":4096,
                         "reasoning":{"effort":"none"},
                         "text":{"format":{"type":"json_schema","name":"structured_response","strict":true,
                         "schema":{"type":"object"}}}}
                        """))
                .andRespond(withSuccess("""
                        {"status":"completed","output":[{"type":"reasoning","summary":[]},
                        {"type":"message","content":[{"type":"output_text","text":"{\\"questions\\":[]}"}]}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(openai().generate(REQUEST)).isEqualTo("{\"questions\":[]}");
        server.verify();
    }

    @Test
    void quotaErrorPreservesSafeStatusAndRetryDelayWithoutSensitiveBodyOrCause() {
        server.expect(requestTo(GEMINI_URI)).andRespond(withStatus(HttpStatusCode.valueOf(429))
                .header("Retry-After", "10").contentType(MediaType.APPLICATION_JSON).body("""
                        {"error":{"status":"RESOURCE_EXHAUSTED","message":"test-secret-key private prompt",
                        "details":[{"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"53798.1s"}]}}
                        """));
        LlmException failure = catchThrowableOfType(() -> gemini().generate(REQUEST), LlmException.class);

        assertThat(failure.provider()).isEqualTo(LlmProvider.GEMINI);
        assertThat(failure.model()).isEqualTo("gemini-3.8-flash");
        assertThat(failure.httpStatus()).isEqualTo(429);
        assertThat(failure.errorCode()).isEqualTo("RESOURCE_EXHAUSTED");
        assertThat(failure.retryable()).isTrue();
        assertThat(failure.retryAfterSeconds()).isEqualTo(53799);
        assertThat(failure.getMessage()).doesNotContain(SECRET, "private prompt");
        assertThat(failure.getCause()).isNull();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 503})
    void transientHttpFailuresCanBeRetried(int status) {
        server.expect(requestTo(OPENAI_URI)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .header("Retry-After", "30").contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"rate_limit_exceeded\"}}"));
        LlmException failure = catchThrowableOfType(() -> openai().generate(REQUEST), LlmException.class);
        assertThat(failure.httpStatus()).isEqualTo(status);
        assertThat(failure.errorCode()).isEqualTo("rate_limit_exceeded");
        assertThat(failure.retryable()).isTrue();
        assertThat(failure.retryAfterSeconds()).isEqualTo(30);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422})
    void permanentHttpFailuresAreNotRetried(int status) {
        server.expect(requestTo(OPENAI_URI)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .contentType(MediaType.TEXT_PLAIN).body("test-secret-key private prompt"));
        LlmException failure = catchThrowableOfType(() -> openai().generate(REQUEST), LlmException.class);
        assertThat(failure.httpStatus()).isEqualTo(status);
        assertThat(failure.retryable()).isFalse();
        assertThat(failure.getMessage()).doesNotContain(SECRET, "private prompt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"insufficient_quota", "billing_hard_limit_reached"})
    void billingQuotaExhaustionNeedsOperatorActionRatherThanRetry(String code) {
        server.expect(requestTo(OPENAI_URI)).andRespond(withStatus(HttpStatusCode.valueOf(429))
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"code\":\"" + code + "\"}}"));
        LlmException failure = catchThrowableOfType(() -> openai().generate(REQUEST), LlmException.class);
        assertThat(failure.httpStatus()).isEqualTo(429);
        assertThat(failure.errorCode()).isEqualTo(code);
        assertThat(failure.retryable()).isFalse();
    }

    @Test
    void networkFailureIsSafeAndRetryable() {
        server.expect(requestTo(GEMINI_URI)).andRespond(withException(new IOException("test-secret-key")));
        LlmException failure = catchThrowableOfType(() -> gemini().generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("TRANSPORT_ERROR");
        assertThat(failure.retryable()).isTrue();
        assertThat(failure.getMessage()).doesNotContain(SECRET);
        assertThat(failure.getCause()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "[]"})
    void missingErrorObjectPreservesHttpFailureInsteadOfNullPointer(String body) {
        server.expect(requestTo(GEMINI_URI)).andRespond(withStatus(HttpStatusCode.valueOf(503))
                .contentType(MediaType.APPLICATION_JSON).body(body));
        LlmException failure = catchThrowableOfType(() -> gemini().generate(REQUEST), LlmException.class);
        assertThat(failure.httpStatus()).isEqualTo(503);
        assertThat(failure.errorCode()).isEqualTo("HTTP_503");
        assertThat(failure.retryable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"max_output_tokens\"}}",
            "{\"status\":\"completed\",\"output\":[]}", "invalid JSON"})
    void incompleteEmptyAndInvalidResponsesNeverReturnPartialJson(String response) {
        server.expect(requestTo(OPENAI_URI)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        LlmException failure = catchThrowableOfType(() -> openai().generate(REQUEST), LlmException.class);
        assertThat(failure.retryable()).isTrue();
    }

    @Test
    void openAiRefusalIsNotRetriedOrExposed() {
        server.expect(requestTo(OPENAI_URI)).andRespond(withSuccess("""
                {"status":"completed","output":[{"type":"message","content":[
                {"type":"refusal","refusal":"private prompt"}]}]}
                """, MediaType.APPLICATION_JSON));
        LlmException failure = catchThrowableOfType(() -> openai().generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("REFUSAL");
        assertThat(failure.retryable()).isFalse();
        assertThat(failure.getMessage()).doesNotContain("private prompt");
    }

    @Test
    void geminiBlockedContentIsNotRetried() {
        server.expect(requestTo(GEMINI_URI)).andRespond(withSuccess("""
                {"promptFeedback":{"blockReason":"SAFETY"}}
                """, MediaType.APPLICATION_JSON));
        LlmException failure = catchThrowableOfType(() -> gemini().generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("CONTENT_BLOCKED");
        assertThat(failure.retryable()).isFalse();
    }

    @Test
    void missingKeysFailBeforeAnyRequestAndAreNotInPropertiesToString() {
        var properties = new LlmProperties(null, null);
        var client = new OpenAiLlmClient(builder.build(), properties.openai(), mapper);
        LlmException failure = catchThrowableOfType(() -> client.generate(REQUEST), LlmException.class);
        assertThat(failure.errorCode()).isEqualTo("MISSING_API_KEY");
        assertThat(failure.retryable()).isFalse();
        assertThat(new LlmProperties.Gemini(SECRET, null, null, null).toString()).doesNotContain(SECRET);
        assertThat(new LlmProperties.OpenAi(SECRET, null, null, null, null).toString()).doesNotContain(SECRET);
        assertThat(REQUEST.toString()).doesNotContain("private prompt");
        server.verify();
    }

    private GeminiLlmClient gemini() {
        return new GeminiLlmClient(builder.baseUrl("https://generativelanguage.googleapis.com").build(),
                new LlmProperties.Gemini(SECRET, null, null, null), mapper);
    }

    private OpenAiLlmClient openai() {
        return new OpenAiLlmClient(builder.baseUrl("https://api.openai.com").build(),
                new LlmProperties.OpenAi(SECRET, null, null, null, null), mapper);
    }
}
