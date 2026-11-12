package com.shoutoutz.api.resourcesearch.domain;

import java.util.Arrays;

public enum ResourceTopicSide {
    COMMON("common"),
    BACKEND("backend"),
    FRONTEND("frontend"),
    MOBILE("mobile");

    private final String code;

    ResourceTopicSide(String code) {
        this.code = code;
    }

    public static ResourceTopicSide fromCode(String code) {
        return Arrays.stream(values())
                .filter(side -> side.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 주제 분야입니다: " + code));
    }
}
