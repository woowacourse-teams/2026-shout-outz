package com.shoutoutz.api.user.domain;

public record ProfileDisplayName(String value) {

    private static final int MAX_LENGTH = 50;

    public ProfileDisplayName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("사용자 표시 이름은 필수입니다.");
        }
        if (value.codePointCount(0, value.length()) > MAX_LENGTH) {
            throw new IllegalArgumentException("사용자 표시 이름은 50자를 초과할 수 없습니다.");
        }
    }
}
