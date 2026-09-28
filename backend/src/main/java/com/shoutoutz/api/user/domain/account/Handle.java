package com.shoutoutz.api.user.domain.account;

import java.util.Locale;
import java.util.regex.Pattern;

public record Handle(String value) {

    public static final String FORMAT_REGEX = "^@[A-Za-z0-9_-]{2,30}$";
    static final Pattern FORMAT_PATTERN = Pattern.compile(FORMAT_REGEX);

    public Handle {
        UserValidator.validateHandle(value);
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
