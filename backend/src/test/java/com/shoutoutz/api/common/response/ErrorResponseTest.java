package com.shoutoutz.api.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ErrorResponseTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("필드 상세 정보가 있는 오류 응답을 생성한다")
    void createsErrorResponseWithDetails() throws Exception {
        List<ErrorResponse.ErrorDetail> details = List.of(
                new ErrorResponse.ErrorDetail("email", "올바른 이메일 형식이 아닙니다."));
        ErrorResponse response = ErrorResponse.error(
                "VALIDATION_FAILED", "입력값을 확인해주세요.", details);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("status").asText()).isEqualTo("error");
        assertThat(json.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(json.get("message").asText()).isEqualTo("입력값을 확인해주세요.");
        assertThat(json.get("details").get(0).get("field").asText()).isEqualTo("email");
        assertThat(json.get("details").get(0).get("message").asText())
                .isEqualTo("올바른 이메일 형식이 아닙니다.");
        assertThat(json.has("data")).isFalse();
        assertThat(json.has("meta")).isFalse();
    }

    @Test
    @DisplayName("필드 오류가 없으면 details를 생략한다")
    void omitsDetailsWhenThereIsNoBodyFieldError() throws Exception {
        ErrorResponse response = ErrorResponse.error(
                "RESOURCE_NOT_FOUND", "자원을 찾을 수 없습니다.");
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("status").asText()).isEqualTo("error");
        assertThat(json.has("details")).isFalse();
    }
}
