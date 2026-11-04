package com.shoutoutz.api.common.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class OpenAiEmbeddingClient implements EmbeddingClient {
    // 한 요청에 넣는 입력 수. API 상한(2,048개, 요청당 30만 토큰)보다 작게 잡는다.
    static final int BATCH_SIZE = 256;

    private final RestClient restClient;
    private final EmbeddingProperties properties;
    private final ObjectMapper mapper;

    public OpenAiEmbeddingClient(RestClient restClient, EmbeddingProperties properties, ObjectMapper mapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("빈 문장은 임베딩할 수 없습니다.");
        }
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            vectors.addAll(request(texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()))));
        }
        return vectors;
    }

    @Override
    public String modelId() {
        return properties.label();
    }

    private List<float[]> request(List<String> inputs) {
        JsonNode data = post(Map.of("model", properties.modelId(), "input", inputs,
                "dimensions", properties.dimensions(), "encoding_format", "float")).path("data");
        if (!data.isArray() || data.size() != inputs.size()) {
            throw new EmbeddingException(200, "INVALID_RESPONSE", true);
        }
        float[][] vectors = new float[inputs.size()][];
        for (JsonNode item : data) {
            int index = item.path("index").asInt(-1);
            if (index < 0 || index >= vectors.length || vectors[index] != null) {
                throw new EmbeddingException(200, "INVALID_RESPONSE", true);
            }
            vectors[index] = vector(item.path("embedding"));
        }
        return List.of(vectors);
    }

    private float[] vector(JsonNode values) {
        if (!values.isArray() || values.size() != properties.dimensions()) {
            throw new EmbeddingException(200, "DIMENSION_MISMATCH", false);
        }
        float[] vector = new float[values.size()];
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) values.get(i).asDouble();
            sum += (double) vector[i] * vector[i];
        }
        if (properties.normalize()) {
            double norm = Math.sqrt(sum);
            if (norm == 0) {
                throw new EmbeddingException(200, "ZERO_VECTOR", false);
            }
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    private JsonNode post(Object body) {
        String key = properties.apiKey();
        if (key == null || key.isBlank()) {
            throw new EmbeddingException(null, "MISSING_API_KEY", false);
        }
        String response;
        try {
            response = restClient.post().uri("/v1/embeddings").header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String code = errorCode(exception.getResponseBodyAsString(), "HTTP_" + status);
            boolean billingFailure = "insufficient_quota".equals(code) || "billing_hard_limit_reached".equals(code);
            throw new EmbeddingException(status, code,
                    !billingFailure && (status == 408 || status == 429 || status >= 500));
        } catch (RestClientException exception) {
            throw new EmbeddingException(null, "TRANSPORT_ERROR", true);
        }
        try {
            JsonNode parsed = response == null || response.isBlank() ? null : mapper.readTree(response);
            if (parsed == null || !parsed.isObject()) {
                throw new EmbeddingException(200, "INVALID_RESPONSE", true);
            }
            return parsed;
        } catch (tools.jackson.core.JacksonException exception) {
            throw new EmbeddingException(200, "INVALID_RESPONSE", true);
        }
    }

    private String errorCode(String body, String fallback) {
        try {
            JsonNode parsed = mapper.readTree(body);
            return parsed == null ? fallback : parsed.path("error").path("code").asString(fallback);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
