package com.shoutoutz.api.news.presentation.validation;

import com.shoutoutz.api.news.presentation.dto.request.NewsUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

final class NewsUpdateRequestValidator implements ConstraintValidator<ValidNewsUpdateRequest, NewsUpdateRequest> {

    @Override
    public boolean isValid(NewsUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;
        valid &= addViolationIfMissing(
                request.isEventStartAtProvided(), "eventStartAt", "eventStartAt은 필수입니다.", context);
        valid &= addViolationIfMissing(
                request.isEventEndAtProvided(), "eventEndAt", "eventEndAt은 필수입니다.", context);
        valid &= addViolationIfMissing(
                request.isCtaProvided(), "cta", "cta는 필수입니다. 삭제하려면 null을 명시해주세요.", context);
        return valid;
    }

    private boolean addViolationIfMissing(
            boolean provided,
            String field,
            String message,
            ConstraintValidatorContext context
    ) {
        if (provided) {
            return true;
        }
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
