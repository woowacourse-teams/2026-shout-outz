package com.shoutoutz.api.auth.domain;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OAuthAccount {

    private static final int MAX_PROVIDER_ACCOUNT_ID_LENGTH = 255;

    private final Long id;
    private final Long userId;
    private final OAuthProvider provider;
    private final String providerAccountId;
    private final String providerAvatarUrl;
    private final Instant lastSyncedAt;
    private final Instant lastLoginAt;

    @Builder
    private OAuthAccount(
            Long id,
            Long userId,
            OAuthProvider provider,
            String providerAccountId,
            String providerAvatarUrl,
            Instant lastSyncedAt,
            Instant lastLoginAt
    ) {
        validate(userId, provider, providerAccountId);
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerAccountId = providerAccountId;
        this.providerAvatarUrl = providerAvatarUrl;
        this.lastSyncedAt = lastSyncedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static OAuthAccount initialize(
            Long userId,
            OAuthProvider provider,
            String providerAccountId,
            String providerAvatarUrl,
            Instant authenticatedAt
    ) {
        validateAuthenticatedAt(authenticatedAt);
        return new OAuthAccount(
                null,
                userId,
                provider,
                providerAccountId,
                providerAvatarUrl,
                authenticatedAt,
                authenticatedAt
        );
    }

    public OAuthAccount recordLogin(String providerAvatarUrl, Instant authenticatedAt) {
        validateAuthenticatedAt(authenticatedAt);
        return new OAuthAccount(
                id,
                userId,
                provider,
                providerAccountId,
                providerAvatarUrl,
                authenticatedAt,
                authenticatedAt
        );
    }

    private void validate(
            Long userId,
            OAuthProvider provider,
            String providerAccountId
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (provider == null) {
            throw new IllegalArgumentException("OAuth Provider는 필수입니다.");
        }
        if (providerAccountId == null || providerAccountId.isBlank()) {
            throw new IllegalArgumentException("Provider 계정 ID는 필수입니다.");
        }
        if (providerAccountId.codePointCount(0, providerAccountId.length())
                > MAX_PROVIDER_ACCOUNT_ID_LENGTH) {
            throw new IllegalArgumentException("Provider 계정 ID는 255자를 초과할 수 없습니다.");
        }
    }

    private static void validateAuthenticatedAt(Instant authenticatedAt) {
        if (authenticatedAt == null) {
            throw new IllegalArgumentException("인증 시각은 필수입니다.");
        }
    }
}
