package com.shoutoutz.api.auth.presentation.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

    /**
     * false이면 비로그인 요청에서 401 대신 null을 주입한다.
     */
    boolean required() default true;
}
