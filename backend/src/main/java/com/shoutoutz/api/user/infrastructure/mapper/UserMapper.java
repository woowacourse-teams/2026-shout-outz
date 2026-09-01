package com.shoutoutz.api.user.infrastructure.mapper;

import com.shoutoutz.api.user.domain.Handle;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.infrastructure.UserEntity;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .handle(user.getHandle().value())
                .status(user.getStatus())
                .role(user.getRole())
                .lastLoginAt(user.getLastLoginAt())
                .deletedAt(user.getDeletedAt())
                .purgedAt(user.getPurgedAt())
                .build();
    }

    public static User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .handle(new Handle(entity.getHandle()))
                .status(entity.getStatus())
                .role(entity.getRole())
                .lastLoginAt(entity.getLastLoginAt())
                .deletedAt(entity.getDeletedAt())
                .purgedAt(entity.getPurgedAt())
                .build();
    }
}
