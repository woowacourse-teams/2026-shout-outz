package com.shoutoutz.api.auth.application;

import com.shoutoutz.api.auth.application.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.application.command.OAuthSignupResult;
import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.exception.UserErrorCode;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthSignupService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    @Transactional
    public OAuthSignupResult signup(OAuthSignupCommand command) {
        OAuthIdentity identity = command.identity();
        if (oauthAccountRepository.findByProviderAndProviderAccountId(
                identity.provider(),
                identity.providerAccountId()
        ).isPresent()) {
            throw new IllegalStateException("이미 가입된 OAuth 계정입니다.");
        }
        if (userRepository.findByHandle(command.handle()).isPresent()) {
            throw new DuplicateEntityException(UserErrorCode.HANDLE_ALREADY_EXISTS);
        }

        Instant authenticatedAt = Instant.now();
        User user = User.initialize(command.handle()).recordLogin(authenticatedAt);
        User savedUser = userRepository.save(user);
        UserProfile userProfile = UserProfile.initialize(savedUser.getId(), command.displayName());
        OAuthAccount oauthAccount = OAuthAccount.initialize(
                savedUser.getId(),
                identity.provider(),
                identity.providerAccountId(),
                identity.avatarUrl(),
                authenticatedAt
        );

        userProfileRepository.save(userProfile);
        oauthAccountRepository.save(oauthAccount);

        return new OAuthSignupResult(savedUser.getId(), savedUser.getRole());
    }
}
