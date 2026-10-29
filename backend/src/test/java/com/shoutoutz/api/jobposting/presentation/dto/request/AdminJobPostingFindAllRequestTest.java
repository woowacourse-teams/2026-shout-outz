package com.shoutoutz.api.jobposting.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminJobPostingFindAllRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("size 가 없으면 20개를 쓰고, 빈 커서는 null 로 둔다.")
    void usesDefaults() {
        AdminJobPostingFindAllRequest request = new AdminJobPostingFindAllRequest(null, " ");

        assertThat(request.resolvedSize()).isEqualTo(20);
        assertThat(request.cursor()).isNull();
    }

    @Test
    @DisplayName("size 는 1부터 100까지 허용한다.")
    void validatesSizeRange() {
        assertThat(validator.validate(new AdminJobPostingFindAllRequest(1, null))).isEmpty();
        assertThat(validator.validate(new AdminJobPostingFindAllRequest(100, null))).isEmpty();
        assertThat(validator.validate(new AdminJobPostingFindAllRequest(0, null))).hasSize(1);
        assertThat(validator.validate(new AdminJobPostingFindAllRequest(101, null))).hasSize(1);
    }
}
