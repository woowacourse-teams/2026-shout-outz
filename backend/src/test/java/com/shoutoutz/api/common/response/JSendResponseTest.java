package com.shoutoutz.api.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JSendResponseTest {

    @Test
    void createsSuccessResponseWithoutCodeAndMessage() {
        JSendResponse<Map<String, Integer>> response = JSendResponse.success(Map.of("id", 1));

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).containsEntry("id", 1);
        assertThat(response.meta()).isNull();
        assertThat(response.code()).isNull();
        assertThat(response.message()).isNull();
        assertThat(response.details()).isNull();
    }

    @Test
    void includesMetaWhenProvided() {
        JSendResponse<Map<String, Integer>> response = JSendResponse.success(
                Map.of("id", 1), Map.of("page", 1));

        assertThat(response.meta()).isEqualTo(Map.of("page", 1));
    }

    @Test
    void createsErrorResponseWithDetails() {
        List<JSendResponse.ErrorDetail> details = List.of(
                new JSendResponse.ErrorDetail("email", "올바른 이메일 형식이 아닙니다."));
        JSendResponse<Void> response = JSendResponse.error(
                "VALIDATION_FAILED", "입력값을 확인해주세요.", details);

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.message()).isEqualTo("입력값을 확인해주세요.");
        assertThat(response.details()).containsExactly(
                new JSendResponse.ErrorDetail("email", "올바른 이메일 형식이 아닙니다."));
        assertThat(response.data()).isNull();
        assertThat(response.meta()).isNull();
    }

    @Test
    void doesNotCreateDetailsWhenThereIsNoBodyFieldError() {
        JSendResponse<Void> response = JSendResponse.error(
                "RESOURCE_NOT_FOUND", "자원을 찾을 수 없습니다.");

        assertThat(response.status()).isEqualTo("error");
        assertThat(response.details()).isNull();
    }
}
