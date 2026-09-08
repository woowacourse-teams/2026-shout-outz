package com.shoutoutz.api.auth.presentation.validation;

import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupRequest;
import com.shoutoutz.api.user.domain.profile.UserType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class OAuthSignupProfileRequestValidator
        implements ConstraintValidator<ValidOAuthSignupProfile, OAuthSignupRequest> {

    @Override
    public boolean isValid(OAuthSignupRequest request, ConstraintValidatorContext context) {
        if (request == null || request.userType() == null) {
            return true;
        }

        return switch (request.userType()) {
            case GENERAL -> validateGeneral(request, context);
            case WOOWACOURSE_CREW -> validateCrew(request, context);
            case WOOWACOURSE_COACH -> validateCoach(request, context);
        };
    }

    private boolean validateGeneral(
            OAuthSignupRequest request,
            ConstraintValidatorContext context
    ) {
        if (request.track() == null && request.cohort() == null) {
            return true;
        }
        addViolation(context, "track", "일반 사용자는 트랙과 기수를 입력할 수 없습니다.");
        return false;
    }

    private boolean validateCrew(
            OAuthSignupRequest request,
            ConstraintValidatorContext context
    ) {
        if (request.track() == null || request.track().isBlank()) {
            addViolation(context, "track", "우아한테크코스 크루의 트랙은 필수입니다.");
            return false;
        }
        if (request.cohort() == null) {
            addViolation(context, "cohort", "우아한테크코스 크루의 기수는 필수입니다.");
            return false;
        }
        return true;
    }

    private boolean validateCoach(
            OAuthSignupRequest request,
            ConstraintValidatorContext context
    ) {
        if (request.cohort() == null) {
            return true;
        }
        addViolation(context, "cohort", "우아한테크코스 코치는 기수를 입력할 수 없습니다.");
        return false;
    }

    private void addViolation(
            ConstraintValidatorContext context,
            String field,
            String message
    ) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
