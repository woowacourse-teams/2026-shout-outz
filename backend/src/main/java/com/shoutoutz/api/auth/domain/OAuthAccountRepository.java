package com.shoutoutz.api.auth.domain;

import java.util.Optional;

public interface OAuthAccountRepository {

    OAuthAccount save(OAuthAccount oauthAccount);

    Optional<OAuthAccount> findByProviderAndProviderAccountId(
            OAuthProvider provider,
            String providerAccountId
    );
}
