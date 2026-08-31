package com.shoutoutz.api.user.domain;

import java.time.Instant;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
public class User {

    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,30}$");

    private final Long id;
    private final String handle;
    private final UserStatus status;
    private final UserRole role;
    private final Instant lastLoginAt;
    private final Instant deletedAt;
    private final Instant purgedAt;

    @Builder
    private User(
            Long id,
            String handle,
            UserStatus status,
            UserRole role,
            Instant lastLoginAt,
            Instant deletedAt,
            Instant purgedAt
    ) {
        validate(handle, status, role, deletedAt);
        this.id = id;
        this.handle = handle;
        this.status = status;
        this.role = role;
        this.lastLoginAt = lastLoginAt;
        this.deletedAt = deletedAt;
        this.purgedAt = purgedAt;
    }

    public static User initialize(String handle) {
        return new User(
                null,
                handle,
                UserStatus.ACTIVE,
                UserRole.USER,
                null,
                null,
                null
        );
    }

    private void validate(
            String handle,
            UserStatus status,
            UserRole role,
            Instant deletedAt
    ) {
        if (handle == null || !HANDLE_PATTERN.matcher(handle).matches()) {
            throw new IllegalArgumentException("유효하지 않은 사용자 핸들입니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("사용자 상태는 필수입니다.");
        }
        if (role == null) {
            throw new IllegalArgumentException("사용자 권한은 필수입니다.");
        }
        if ((status == UserStatus.DELETED) != (deletedAt != null)) {
            throw new IllegalArgumentException("탈퇴 상태와 탈퇴 시각은 함께 존재해야 합니다.");
        }
    }
}
