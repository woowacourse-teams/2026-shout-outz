package com.shoutoutz.api.news.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이벤트 기간의 순서 유효성을 검증하는
 * 커스텀 Bean Validation 어노테이션.
 *
 * Bean Validation과 동일하게, MethodArgumentNotValidException가 발생한다.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EventPeriodRequestValidator.class)
public @interface ValidEventPeriod {

    String message() default "eventStartAt은 eventEndAt보다 늦을 수 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
