package com.dropit.backend.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ThumbnailVariant {
    RETRO("retro"),
    FOOD("food"),
    CODE("code"),
    ROULETTE("roulette"),
    CSS("css"),
    TEMPERATURE("temperature"),
    GARDEN("garden"),
    DUNGEON("dungeon"),
    NAMING("naming"),
    HTTP("http"),
    TIMER("timer"),
    MUSEUM("museum"),
    NEW("new");

    private final String value;

    ThumbnailVariant(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ThumbnailVariant fromValue(String value) {
        for (ThumbnailVariant variant : values()) {
            if (variant.value.equals(value)) {
                return variant;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 썸네일 변형입니다: " + value);
    }
}
