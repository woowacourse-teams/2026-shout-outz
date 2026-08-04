package com.dropit.backend.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AppCategory {
    GAME("게임"),
    PRODUCTIVITY("생산성"),
    LEARNING("학습"),
    TRAVEL("여행"),
    AI("AI"),
    HARNESS("하네스"),
    SELF_DEVELOPMENT("자기개발"),
    DEVELOPMENT("개발"),
    DESIGN("디자인"),
    LIFESTYLE("생활"),
    HEALTH("건강"),
    GENERATOR("생성기"),
    SOCIAL("소셜"),
    EXPERIMENT("실험");

    private final String value;

    AppCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static AppCategory fromValue(String value) {
        for (AppCategory category : values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 카테고리입니다: " + value);
    }
}
