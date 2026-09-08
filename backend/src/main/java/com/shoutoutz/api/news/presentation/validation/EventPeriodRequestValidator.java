package com.shoutoutz.api.news.presentation.validation;

import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

final class EventPeriodRequestValidator implements ConstraintValidator<ValidEventPeriod, EventCreateRequest> {

    @Override
    public boolean isValid(EventCreateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.eventStartAt() == null || request.eventEndAt() == null) {
            return true;
        }
        if (!request.eventStartAt().isAfter(request.eventEndAt())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("eventStartAt")
                .addConstraintViolation();
        return false;
    }
}
