package com.shoutoutz.api.auth.infrastructure.mapper;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountEntity;

public final class OAuthAccountMapper {

    private OAuthAccountMapper() {
    }

    public static OAuthAccountEntity toEntity(OAuthAccount oauthAccount) {
        return OAuthAccountEntity.builder()
                .id(oauthAccount.getId())
                .userId(oauthAccount.getUserId())
                .provider(oauthAccount.getProvider())
                .providerAccountId(oauthAccount.getProviderAccountId())
                .providerAvatarUrl(oauthAccount.getProviderAvatarUrl())
                .lastSyncedAt(oauthAccount.getLastSyncedAt())
                .lastLoginAt(oauthAccount.getLastLoginAt())
                .build();
    }

    public static OAuthAccount toDomain(OAuthAccountEntity entity) {
        return OAuthAccount.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .provider(entity.getProvider())
                .providerAccountId(entity.getProviderAccountId())
                .providerAvatarUrl(entity.getProviderAvatarUrl())
                .lastSyncedAt(entity.getLastSyncedAt())
                .lastLoginAt(entity.getLastLoginAt())
                .build();
    }
}
