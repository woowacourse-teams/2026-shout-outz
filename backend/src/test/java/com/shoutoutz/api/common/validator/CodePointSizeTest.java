package com.shoutoutz.api.common.validator;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodePointSizeTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("보조 평면 문자를 UTF-16 길이가 아닌 유니코드 코드 포인트로 센다")
    void countsUnicodeCodePoints() {
        assertThat(validator.validate(new Value("😀"))).isEmpty();
        assertThat(validator.validate(new Value("😀😀"))).hasSize(1);
    }

    private record Value(
            @CodePointSize(max = 1) String value
    ) {
    }
}
