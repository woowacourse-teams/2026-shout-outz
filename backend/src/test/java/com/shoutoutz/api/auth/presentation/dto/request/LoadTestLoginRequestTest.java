package com.shoutoutz.api.auth.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LoadTestLoginRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("handle이 비어 있으면 검증에 실패한다.")
    void rejectsBlankHandle(String handle) {
        LoadTestLoginRequest request = new LoadTestLoginRequest(handle);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("handle");
    }

    @Test
    @DisplayName("@ 접두사가 없는 handle도 허용한다.")
    void acceptsHandleWithoutPrefix() {
        LoadTestLoginRequest request = new LoadTestLoginRequest("lt-00001");

        assertThat(validator.validate(request)).isEmpty();
    }
}
