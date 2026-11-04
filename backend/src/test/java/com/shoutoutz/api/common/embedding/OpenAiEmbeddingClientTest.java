package com.shoutoutz.api.common.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class OpenAiEmbeddingClientTest {
    private static final String URI = "https://api.openai.com/v1/embeddings";
    private static final String SECRET = "test-embedding-key";
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private MockRestServiceServer server;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void embedSendsModelAndDimensionsAndReturnsUnitVector() {
        server.expect(requestTo(URI)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + SECRET))
                .andExpect(content().json("""
                        {"model":"text-embedding-3-small","input":["무중단 배포 경험"],"dimensions":2,
                         "encoding_format":"float"}
                        """))
                .andRespond(withSuccess("""
                        {"data":[{"index":0,"embedding":[3.0,4.0]}]}
                        """, MediaType.APPLICATION_JSON));

        float[] vector = client(properties(SECRET, 2, true)).embed("무중단 배포 경험");

        assertThat(vector).containsExactly(0.6f, 0.8f);
        server.verify();
    }

    @Test
    void embedAllKeepsInputOrderEvenWhenResponseIsShuffled() {
        server.expect(requestTo(URI)).andRespond(withSuccess("""
                {"data":[{"index":1,"embedding":[0.0,1.0]},{"index":0,"embedding":[1.0,0.0]}]}
                """, MediaType.APPLICATION_JSON));

        List<float[]> vectors = client(properties(SECRET, 2, true)).embedAll(List.of("첫째", "둘째"));

        assertThat(vectors.get(0)).containsExactly(1.0f, 0.0f);
        assertThat(vectors.get(1)).containsExactly(0.0f, 1.0f);
    }

    @Test
    void embedAllSplitsLargeInputIntoBatches() {
        int total = OpenAiEmbeddingClient.BATCH_SIZE + 1;
        server.expect(times(1), requestTo(URI)).andRespond(withSuccess(response(OpenAiEmbeddingClient.BATCH_SIZE),
                MediaType.APPLICATION_JSON));
        server.expect(times(1), requestTo(URI)).andRespond(withSuccess(response(1), MediaType.APPLICATION_JSON));

        List<float[]> vectors = client(properties(SECRET, 2, true))
                .embedAll(IntStream.range(0, total).mapToObj(i -> "문장 " + i).toList());

        assertThat(vectors).hasSize(total);
        server.verify();
    }

    @Test
    void normalizeFalseReturnsRawVector() {
        server.expect(requestTo(URI)).andRespond(withSuccess("""
                {"data":[{"index":0,"embedding":[3.0,4.0]}]}
                """, MediaType.APPLICATION_JSON));

        assertThat(client(properties(SECRET, 2, false)).embed("문장")).containsExactly(3.0f, 4.0f);
    }

    @Test
    void modelIdIncludesDimensionsToMatchStoredEmbeddingModel() {
        assertThat(client(properties(SECRET, 1024, true)).modelId()).isEqualTo("text-embedding-3-small@1024");
    }

    @Test
    void responseWithWrongDimensionsIsRejected() {
        server.expect(requestTo(URI)).andRespond(withSuccess("""
                {"data":[{"index":0,"embedding":[1.0,0.0,0.0]}]}
                """, MediaType.APPLICATION_JSON));

        EmbeddingException exception = catchThrowableOfType(EmbeddingException.class,
                () -> client(properties(SECRET, 2, true)).embed("문장"));

        assertThat(exception.errorCode()).isEqualTo("DIMENSION_MISMATCH");
        assertThat(exception.retryable()).isFalse();
    }

    @Test
    void missingKeyFailsWithoutCallingApi() {
        server.expect(never(), requestTo(URI));

        EmbeddingException exception = catchThrowableOfType(EmbeddingException.class,
                () -> client(properties(" ", 2, true)).embed("문장"));

        assertThat(exception.errorCode()).isEqualTo("MISSING_API_KEY");
        server.verify();
    }

    @Test
    void rateLimitIsRetryableButQuotaIsNotAndBodyIsNotExposed() {
        server.expect(requestTo(URI)).andRespond(withStatus(HttpStatusCode.valueOf(429))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"rate_limit_exceeded\",\"message\":\"secret input\"}}"));
        server.expect(requestTo(URI)).andRespond(withStatus(HttpStatusCode.valueOf(429))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"insufficient_quota\"}}"));
        OpenAiEmbeddingClient client = client(properties(SECRET, 2, true));

        EmbeddingException rateLimited = catchThrowableOfType(EmbeddingException.class, () -> client.embed("문장"));
        EmbeddingException quota = catchThrowableOfType(EmbeddingException.class, () -> client.embed("문장"));

        assertThat(rateLimited.retryable()).isTrue();
        assertThat(rateLimited.httpStatus()).isEqualTo(429);
        assertThat(rateLimited.getMessage()).doesNotContain("secret input", SECRET);
        assertThat(rateLimited.getCause()).isNull();
        assertThat(quota.errorCode()).isEqualTo("insufficient_quota");
        assertThat(quota.retryable()).isFalse();
    }

    @Test
    void blankTextIsRejectedBeforeCallingApi() {
        server.expect(never(), requestTo(URI));

        assertThatThrownBy(() -> client(properties(SECRET, 2, true)).embedAll(List.of("문장", " ")))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    void propertiesRejectUnsupportedProviderAndInvalidDimensions() {
        assertThatThrownBy(() -> new EmbeddingProperties("gemini", null, 1024, true, SECRET, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmbeddingProperties("openai", null, 0, true, SECRET, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new EmbeddingProperties(null, null, 1024, true, SECRET, null, null).toString())
                .doesNotContain(SECRET);
    }

    private OpenAiEmbeddingClient client(EmbeddingProperties properties) {
        return new OpenAiEmbeddingClient(builder.baseUrl("https://api.openai.com").build(), properties, mapper);
    }

    private EmbeddingProperties properties(String key, int dimensions, boolean normalize) {
        return new EmbeddingProperties("openai", "text-embedding-3-small", dimensions, normalize, key,
                Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    private String response(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "{\"index\":" + i + ",\"embedding\":[1.0,0.0]}")
                .reduce((a, b) -> a + "," + b)
                .map(items -> "{\"data\":[" + items + "]}")
                .orElseThrow();
    }
}
