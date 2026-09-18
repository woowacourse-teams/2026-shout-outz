package com.shoutoutz.api.visitor.presentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청한 방문자의 {@link com.shoutoutz.api.visitor.domain.VisitorKey}를 주입받는다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Visitor {
}
