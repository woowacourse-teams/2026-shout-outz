package com.shoutoutz.api.auth.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthAccountMapperTest {

    private static final Instant LAST_SYNCED_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant LAST_LOGIN_AT = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    @DisplayName("OAuth 계정 도메인을 엔티티로 변환한다")
    void mapsDomainToEntity() {
        OAuthAccount account = OAuthAccount.builder()
                .id(10L)
                .userId(1L)
                .provider(OAuthProvider.GITHUB)
                .providerAccountId("12345678")
                .providerAvatarUrl("https://avatars.githubusercontent.com/u/12345678")
                .lastSyncedAt(LAST_SYNCED_AT)
                .lastLoginAt(LAST_LOGIN_AT)
                .build();

        OAuthAccountEntity entity = OAuthAccountMapper.toEntity(account);

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getUserId()).isEqualTo(1L);
        assertThat(entity.getProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(entity.getProviderAccountId()).isEqualTo("12345678");
        assertThat(entity.getProviderAvatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/12345678");
        assertThat(entity.getLastSyncedAt()).isEqualTo(LAST_SYNCED_AT);
        assertThat(entity.getLastLoginAt()).isEqualTo(LAST_LOGIN_AT);
    }

    @Test
    @DisplayName("OAuth 계정 엔티티를 도메인으로 변환한다")
    void mapsEntityToDomain() {
        OAuthAccountEntity entity = OAuthAccountEntity.builder()
                .id(10L)
                .userId(1L)
                .provider(OAuthProvider.GITHUB)
                .providerAccountId("12345678")
                .providerAvatarUrl("https://avatars.githubusercontent.com/u/12345678")
                .lastSyncedAt(LAST_SYNCED_AT)
                .lastLoginAt(LAST_LOGIN_AT)
                .build();

        OAuthAccount account = OAuthAccountMapper.toDomain(entity);

        assertThat(account.getId()).isEqualTo(10L);
        assertThat(account.getUserId()).isEqualTo(1L);
        assertThat(account.getProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(account.getProviderAccountId()).isEqualTo("12345678");
        assertThat(account.getProviderAvatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/12345678");
        assertThat(account.getLastSyncedAt()).isEqualTo(LAST_SYNCED_AT);
        assertThat(account.getLastLoginAt()).isEqualTo(LAST_LOGIN_AT);
    }
}
