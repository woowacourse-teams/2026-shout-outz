package com.shoutoutz.api.jobposting.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.domain.ReanalysisScope;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AdminJobPostingReanalysisRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @EnumSource(ReanalysisScope.class)
    @DisplayName("범위가 있으면 통과한다.")
    void acceptsScope(ReanalysisScope scope) {
        assertThat(validator.validate(new AdminJobPostingReanalysisRequest(scope))).isEmpty();
    }

    @Test
    @DisplayName("범위가 없으면 거절한다.")
    void rejectsMissingScope() {
        assertThat(validator.validate(new AdminJobPostingReanalysisRequest(null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("scope");
    }
}
