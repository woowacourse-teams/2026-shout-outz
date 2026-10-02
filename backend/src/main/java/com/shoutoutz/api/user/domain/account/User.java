package com.shoutoutz.api.user.domain.account;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
public class User {

    private final Long id;
    private final Handle handle;
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
        UserValidator.validateUser(status, role, deletedAt);
        this.id = id;
        this.handle = new Handle(handle);
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

    public User recordLogin(Instant loginAt) {
        UserValidator.validateLogin(status, purgedAt, loginAt);

        return new User(
                id,
                handle.value(),
                UserStatus.ACTIVE,
                role,
                loginAt,
                null,
                purgedAt
        );
    }

    public boolean isDeleted() {
        return status == UserStatus.DELETED;
    }
}
