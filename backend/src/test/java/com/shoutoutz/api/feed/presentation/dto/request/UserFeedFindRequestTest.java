package com.shoutoutz.api.feed.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserFeedFindRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 파라미터를_입력하지_않으면_첫_페이지를_20개_조회한다() {
        UserFeedFindRequest request = new UserFeedFindRequest(null, null);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.resolvedSize()).isEqualTo(20);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    void 조회_개수는_1_이상_50_이하로_입력할_수_있다(int size) {
        UserFeedFindRequest request = new UserFeedFindRequest(null, size);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.resolvedSize()).isEqualTo(size);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    void 조회_개수가_범위를_벗어나면_검증에_실패한다(int size) {
        UserFeedFindRequest request = new UserFeedFindRequest(null, size);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void 공백뿐인_커서는_첫_페이지로_본다() {
        UserFeedFindRequest request = new UserFeedFindRequest(" ", null);

        assertThat(request.cursor()).isNull();
    }
}
