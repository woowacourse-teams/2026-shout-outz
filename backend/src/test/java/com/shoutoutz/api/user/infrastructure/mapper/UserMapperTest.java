package com.shoutoutz.api.user.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRole;
import com.shoutoutz.api.user.domain.UserStatus;
import com.shoutoutz.api.user.infrastructure.UserEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    @Test
    @DisplayName("사용자 도메인을 엔티티로 변환한다")
    void mapsDomainToEntity() {
        User user = User.initialize("zzaekkii");

        UserEntity entity = UserMapper.toEntity(user);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getHandle()).isEqualTo("zzaekkii");
        assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(entity.getRole()).isEqualTo(UserRole.USER);
        assertThat(entity.getLastLoginAt()).isNull();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getPurgedAt()).isNull();
    }

    @Test
    @DisplayName("사용자 엔티티를 도메인으로 변환한다")
    void mapsEntityToDomain() {
        Instant deletedAt = Instant.parse("2026-08-31T00:00:00Z");
        Instant purgedAt = Instant.parse("2026-09-30T00:00:00Z");
        UserEntity entity = UserEntity.builder()
                .id(1L)
                .handle("zzaekkii")
                .status(UserStatus.DELETED)
                .role(UserRole.USER)
                .lastLoginAt(Instant.parse("2026-08-30T00:00:00Z"))
                .deletedAt(deletedAt)
                .purgedAt(purgedAt)
                .build();

        User user = UserMapper.toDomain(entity);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getHandle()).isEqualTo("zzaekkii");
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getLastLoginAt()).isEqualTo(Instant.parse("2026-08-30T00:00:00Z"));
        assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(user.getPurgedAt()).isEqualTo(purgedAt);
    }
}
