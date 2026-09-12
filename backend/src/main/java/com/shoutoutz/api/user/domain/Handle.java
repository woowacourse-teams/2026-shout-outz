package com.shoutoutz.api.user.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Handle(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,30}$");

    public Handle {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("유효하지 않은 사용자 핸들입니다.");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        return object instanceof Handle handle
                && value.equalsIgnoreCase(handle.value);
    }

    @Override
    public int hashCode() {
        return value.toLowerCase(Locale.ROOT).hashCode();
    }
}
