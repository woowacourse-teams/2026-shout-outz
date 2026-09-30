package com.shoutoutz.api.auth.domain;

import java.util.Optional;

public interface OAuthAccountRepository {

    OAuthAccount save(OAuthAccount oauthAccount);

    Optional<OAuthAccount> findByUserIdAndProvider(
            long userId,
            OAuthProvider provider
    );

    Optional<OAuthAccount> findByProviderAndProviderAccountId(
            OAuthProvider provider,
            String providerAccountId
    );
}
