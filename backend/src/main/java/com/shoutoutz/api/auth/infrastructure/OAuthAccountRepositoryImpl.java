package com.shoutoutz.api.auth.infrastructure;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountEntity;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountJpaRepository;
import com.shoutoutz.api.auth.infrastructure.mapper.OAuthAccountMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OAuthAccountRepositoryImpl implements OAuthAccountRepository {

    private final OAuthAccountJpaRepository oauthAccountJpaRepository;

    @Override
    public OAuthAccount save(OAuthAccount oauthAccount) {
        OAuthAccountEntity entity = OAuthAccountMapper.toEntity(oauthAccount);
        OAuthAccountEntity savedEntity = oauthAccountJpaRepository.save(entity);

        return OAuthAccountMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<OAuthAccount> findByProviderAndProviderAccountId(
            OAuthProvider provider,
            String providerAccountId
    ) {
        return oauthAccountJpaRepository.findByProviderAndProviderAccountId(
                        provider,
                        providerAccountId
                )
                .map(OAuthAccountMapper::toDomain);
    }
}
