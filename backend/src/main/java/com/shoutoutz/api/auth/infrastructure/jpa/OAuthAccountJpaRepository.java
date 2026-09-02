package com.shoutoutz.api.auth.infrastructure.jpa;

import com.shoutoutz.api.auth.domain.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccountEntity, Long> {

    Optional<OAuthAccountEntity> findByProviderAndProviderAccountId(
            OAuthProvider provider,
            String providerAccountId
    );
}
