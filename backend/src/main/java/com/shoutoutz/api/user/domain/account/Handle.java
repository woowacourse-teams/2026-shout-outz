package com.shoutoutz.api.user.domain.account;

import java.util.Locale;

public record Handle(String value) {

    public Handle {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("사용자 핸들은 필수입니다.");
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
