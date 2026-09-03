package com.shoutoutz.api.auth.domain;

public record OAuthIdentity(
        OAuthProvider provider,
        String providerAccountId,
        String avatarUrl,
        String providerProfileUrl
) {

    private static final int MAX_PROVIDER_ACCOUNT_ID_LENGTH = 255;

    public OAuthIdentity(
            OAuthProvider provider,
            String providerAccountId,
            String avatarUrl
    ) {
        this(provider, providerAccountId, avatarUrl, null);
    }

    public OAuthIdentity {
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
}
