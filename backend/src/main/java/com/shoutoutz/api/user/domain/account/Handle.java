package com.shoutoutz.api.user.domain.account;

import java.util.Locale;

public record Handle(String value) {

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
