package com.shoutoutz.api.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SuccessResponseTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("meta 없이 성공 응답을 생성한다")
    void createsSuccessResponseWithoutMeta() throws Exception {
        SuccessResponse<Map<String, Integer>> response = SuccessResponse.success(Map.of("id", 1));
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("status").asText()).isEqualTo("success");
        assertThat(json.get("data").get("id").asInt()).isEqualTo(1);
        assertThat(json.has("meta")).isFalse();
    }

    @Test
    @DisplayName("meta가 전달되면 성공 응답에 포함한다")
    void includesMetaWhenProvided() throws Exception {
        SuccessResponse<Map<String, Integer>> response = SuccessResponse.success(
                Map.of("id", 1), Map.of("page", 1));
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("meta").get("page").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("data가 null이어도 data 필드를 유지한다")
    void keepsDataFieldWhenDataIsNull() throws Exception {
        SuccessResponse<Void> response = SuccessResponse.success(null);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.has("data")).isTrue();
        assertThat(json.get("data").isNull()).isTrue();
    }
}
