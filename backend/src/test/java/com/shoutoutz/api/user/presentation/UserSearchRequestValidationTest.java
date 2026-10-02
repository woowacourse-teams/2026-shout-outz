package com.shoutoutz.api.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.presentation.dto.request.UserSearchRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class UserSearchRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @DisplayName("검색어는 필수다")
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void requireKeyword(String keyword) {
        UserSearchRequest request = new UserSearchRequest(keyword, null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("keyword");
    }

    @Test
    @DisplayName("검색어 길이를 유니코드 코드 포인트 기준으로 검증한다")
    void validateKeywordLength() {
        UserSearchRequest request = new UserSearchRequest("😀".repeat(51), null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("keyword");
    }

    @ParameterizedTest
    @DisplayName("검색 결과 개수는 1개 이상 100개 이하다")
    @ValueSource(ints = {0, 101})
    void validateSize(int size) {
        UserSearchRequest request = new UserSearchRequest("재키", null, size);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("size");
    }

    @Test
    @DisplayName("검색 결과 개수를 생략하면 20개를 사용한다")
    void useDefaultSize() {
        UserSearchRequest request = new UserSearchRequest("재키", null, null);

        assertThat(request.resolvedSize()).isEqualTo(20);
    }
}
