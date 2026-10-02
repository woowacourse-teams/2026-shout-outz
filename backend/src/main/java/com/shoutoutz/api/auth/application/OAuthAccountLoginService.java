package com.shoutoutz.api.auth.application;

import com.shoutoutz.api.auth.application.command.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthAccountLoginService {

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;

    @Transactional
    public OAuthLoginCallbackResult completeLogin(
            OAuthIdentity identity,
            Instant authenticatedAt
    ) {
        return oauthAccountRepository.findByProviderAndProviderAccountId(
                        identity.provider(),
                        identity.providerAccountId()
                )
                .map(account -> loginExistingUser(account, identity, authenticatedAt))
                .orElseGet(() -> OAuthLoginCallbackResult.signupRequired(identity));
    }

    private OAuthLoginCallbackResult loginExistingUser(
            OAuthAccount account,
            OAuthIdentity identity,
            Instant authenticatedAt
    ) {
        User user = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new IllegalStateException("OAuth 계정의 사용자가 없습니다."));
        User loggedInUser = user.recordLogin(authenticatedAt);
        OAuthAccount loggedInAccount = account.recordLogin(identity.avatarUrl(), authenticatedAt);

        userRepository.save(loggedInUser);
        oauthAccountRepository.save(loggedInAccount);

        return OAuthLoginCallbackResult.authenticated(
                loggedInUser.getId(),
                loggedInUser.getRole()
        );
    }
}
