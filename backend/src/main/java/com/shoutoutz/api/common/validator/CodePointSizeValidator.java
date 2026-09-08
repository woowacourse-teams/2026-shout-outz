package com.shoutoutz.api.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CodePointSizeValidator implements ConstraintValidator<CodePointSize, CharSequence> {

    private int min;
    private int max;

    @Override
    public void initialize(CodePointSize constraintAnnotation) {
        min = constraintAnnotation.min();
        max = constraintAnnotation.max();
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("CodePointSize의 범위가 올바르지 않습니다.");
        }
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String stringValue = value.toString();
        int length = stringValue.codePointCount(0, stringValue.length());
        return min <= length && length <= max;
    }
}
