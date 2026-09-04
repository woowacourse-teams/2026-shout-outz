package com.shoutoutz.api.common.exception.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.ConstraintViolation;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.FieldError;

@Getter
@Builder
@RequiredArgsConstructor
public class ErrorResponse {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String code;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ValidationError> errors;

    @Builder
    public record ValidationError(String code, String message) {
        public static ValidationError of(final ConstraintViolation<?> violation, ErrorCode code) {
            return ValidationError.builder().code(code.name()).message(violation.getMessage()).build();
        }

        public static ValidationError of(final FieldError fieldError, ErrorCode code) {
            return ValidationError.builder()
                    .code(code.name())
                    .message(fieldError.getDefaultMessage())
                    .build();
        }
    }
}
