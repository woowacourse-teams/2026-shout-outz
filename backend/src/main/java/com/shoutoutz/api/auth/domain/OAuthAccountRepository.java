package com.shoutoutz.api.auth.domain;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface OAuthAccountRepository {

    OAuthAccount save(OAuthAccount oauthAccount);

    Optional<OAuthAccount> findByUserIdAndProvider(
            long userId,
            OAuthProvider provider
    );

    List<OAuthAccount> findAllByUserIdsAndProvider(
            Collection<Long> userIds,
            OAuthProvider provider
    );

    Optional<OAuthAccount> findByProviderAndProviderAccountId(
            OAuthProvider provider,
            String providerAccountId
    );
}
