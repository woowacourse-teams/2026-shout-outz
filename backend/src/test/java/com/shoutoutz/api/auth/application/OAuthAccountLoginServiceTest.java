package com.shoutoutz.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.domain.UserRole;
import com.shoutoutz.api.user.domain.UserStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthAccountLoginServiceTest {

    private static final Instant AUTHENTICATED_AT = Instant.parse("2026-09-03T00:00:00Z");

    private final OAuthAccountRepository oauthAccountRepository =
            mock(OAuthAccountRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private OAuthAccountLoginService oauthAccountLoginService;

    @BeforeEach
    void setUp() {
        oauthAccountLoginService = new OAuthAccountLoginService(
                oauthAccountRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("연결된 OAuth 계정이 있으면 기존 사용자 로그인을 완료한다")
    void completesExistingUserLogin() {
        OAuthIdentity identity = githubIdentity();
        OAuthAccount account = oauthAccount(identity, 1L);
        User user = activeUser(1L);
        given(oauthAccountRepository.findByProviderAndProviderAccountId(
                identity.provider(),
                identity.providerAccountId()
        )).willReturn(Optional.of(account));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        OAuthLoginCallbackResult result = oauthAccountLoginService.completeLogin(
                identity,
                AUTHENTICATED_AT
        );

        assertThat(result.status()).isEqualTo(OAuthLoginCallbackResult.Status.AUTHENTICATED);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo(UserRole.USER);
        assertThat(result.identity()).isNull();
        verify(oauthAccountRepository).save(org.mockito.ArgumentMatchers.any());
        verify(userRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("연결된 OAuth 계정이 없으면 신규 가입이 필요하다")
    void requiresSignupForNewOAuthIdentity() {
        OAuthIdentity identity = githubIdentity();
        given(oauthAccountRepository.findByProviderAndProviderAccountId(
                identity.provider(),
                identity.providerAccountId()
        )).willReturn(Optional.empty());

        OAuthLoginCallbackResult result = oauthAccountLoginService.completeLogin(
                identity,
                AUTHENTICATED_AT
        );

        assertThat(result.status()).isEqualTo(OAuthLoginCallbackResult.Status.SIGNUP_REQUIRED);
        assertThat(result.identity()).isEqualTo(identity);
        assertThat(result.userId()).isNull();
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("정지된 사용자의 OAuth 로그인을 거부한다")
    void rejectsBannedUserLogin() {
        OAuthIdentity identity = githubIdentity();
        OAuthAccount account = oauthAccount(identity, 1L);
        User bannedUser = User.builder()
                .id(1L)
                .handle("sangjun")
                .status(UserStatus.BANNED)
                .role(UserRole.USER)
                .build();
        given(oauthAccountRepository.findByProviderAndProviderAccountId(
                identity.provider(),
                identity.providerAccountId()
        )).willReturn(Optional.of(account));
        given(userRepository.findById(1L)).willReturn(Optional.of(bannedUser));

        assertThatThrownBy(() -> oauthAccountLoginService.completeLogin(
                identity,
                AUTHENTICATED_AT
        )).isInstanceOf(IllegalStateException.class);

        verify(oauthAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private OAuthAccount oauthAccount(OAuthIdentity identity, Long userId) {
        return OAuthAccount.builder()
                .id(10L)
                .userId(userId)
                .provider(identity.provider())
                .providerAccountId(identity.providerAccountId())
                .build();
    }

    private User activeUser(Long id) {
        return User.builder()
                .id(id)
                .handle("sangjun")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }

    private OAuthIdentity githubIdentity() {
        return new OAuthIdentity(
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678"
        );
    }
}
