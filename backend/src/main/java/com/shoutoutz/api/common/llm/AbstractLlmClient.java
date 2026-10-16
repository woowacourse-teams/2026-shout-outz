package com.shoutoutz.api.common.llm;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

abstract class AbstractLlmClient implements LlmClient {
    protected final RestClient restClient;
    protected final ObjectMapper mapper;
    protected final String model;

    AbstractLlmClient(RestClient restClient, ObjectMapper mapper, String model) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.model = model;
    }

    protected JsonNode post(String uri, String keyHeader, String key, Object body) {
        if (key == null || key.isBlank()) {
            throw failure(null, "MISSING_API_KEY", false);
        }
        String response;
        try {
            response = restClient.post().uri(uri).header(keyHeader, key)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            JsonNode error = parseError(exception.getResponseBodyAsString());
            String code = error.path("error").path(provider() == LlmProvider.GEMINI ? "status" : "code")
                    .asString("HTTP_" + status);
            boolean billingFailure = provider() == LlmProvider.OPENAI
                    && Set.of("insufficient_quota", "billing_hard_limit_reached").contains(code);
            throw new LlmException(provider(), model, status, code,
                    !billingFailure && (status == 408 || status == 429 || status >= 500),
                    retryAfter(exception.getResponseHeaders(), error));
        } catch (RestClientException exception) {
            throw failure(null, "TRANSPORT_ERROR", true);
        }
        try {
            if (response == null || response.isBlank()) {
                throw failure(200, "EMPTY_RESPONSE", true);
            }
            JsonNode parsed = mapper.readTree(response);
            if (parsed == null || !parsed.isObject()) {
                throw failure(200, "INVALID_RESPONSE", true);
            }
            return parsed;
        } catch (tools.jackson.core.JacksonException exception) {
            throw failure(200, "INVALID_RESPONSE", true);
        }
    }

    protected LlmException failure(Integer status, String code, boolean retryable) {
        return new LlmException(provider(), model, status, code, retryable, 0);
    }

    private JsonNode parseError(String body) {
        try {
            JsonNode parsed = mapper.readTree(body);
            return parsed == null || !parsed.isObject() ? mapper.createObjectNode() : parsed;
        } catch (RuntimeException exception) {
            return mapper.createObjectNode();
        }
    }

    private long retryAfter(HttpHeaders headers, JsonNode body) {
        long delay = 0;
        String header = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (header != null) {
            try {
                delay = Math.max(0, Long.parseLong(header.strip()));
            } catch (NumberFormatException exception) {
                try {
                    Instant retryAt = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                    delay = Math.max(0, Duration.between(Instant.now(), retryAt).getSeconds() + 1);
                } catch (RuntimeException ignored) {
                    // 유효하지 않은 헤더는 재시도 시간을 결정하는 데 사용하지 않는다.
                }
            }
        }
        for (JsonNode detail : body.path("error").path("details")) {
            if ("type.googleapis.com/google.rpc.RetryInfo".equals(detail.path("@type").asString())) {
                String seconds = detail.path("retryDelay").asString("");
                if (seconds.matches("[0-9]{1,12}(\\.[0-9]{1,9})?s")) {
                    delay = Math.max(delay, (long) Math.ceil(Double.parseDouble(seconds.substring(0, seconds.length() - 1))));
                }
            }
        }
        return delay;
    }
}
