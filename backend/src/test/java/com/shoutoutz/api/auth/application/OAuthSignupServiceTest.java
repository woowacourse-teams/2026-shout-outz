package com.shoutoutz.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.auth.application.dto.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.application.dto.result.OAuthSignupResult;
import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OAuthSignupServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository userProfileRepository =
            mock(UserProfileRepository.class);
    private final OAuthAccountRepository oauthAccountRepository =
            mock(OAuthAccountRepository.class);

    private OAuthSignupService oauthSignupService;

    @BeforeEach
    void setUp() {
        oauthSignupService = new OAuthSignupService(
                userRepository,
                userProfileRepository,
                oauthAccountRepository
        );
    }

    @Test
    @DisplayName("가입 정보와 OAuth 신원으로 사용자와 프로필 및 OAuth 계정을 생성한다")
    void signsUpOAuthUser() {
        OAuthSignupCommand command = signupCommand();
        given(oauthAccountRepository.findByProviderAndProviderAccountId(
                OAuthProvider.GITHUB,
                "12345678"
        )).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser());

        OAuthSignupResult result = oauthSignupService.signup(command);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo(UserRole.USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        ArgumentCaptor<OAuthAccount> accountCaptor = ArgumentCaptor.forClass(OAuthAccount.class);
        verify(userRepository).save(userCaptor.capture());
        verify(userProfileRepository).save(profileCaptor.capture());
        verify(oauthAccountRepository).save(accountCaptor.capture());

        User user = userCaptor.getValue();
        UserProfile profile = profileCaptor.getValue();
        OAuthAccount account = accountCaptor.getValue();
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(profile.getUserId()).isEqualTo(1L);
        assertThat(profile.getDisplayName().value()).isEqualTo("상준");
        assertThat(profile.getUserType()).isEqualTo(UserType.GENERAL);
        assertThat(profile.getTrack()).isNull();
        assertThat(profile.getCohort()).isNull();
        assertThat(profile.getAvatarImageId()).isNull();
        assertThat(profile.getGithubProfileUrl()).isNull();
        assertThat(account.getUserId()).isEqualTo(1L);
        assertThat(account.getProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(account.getProviderAccountId()).isEqualTo("12345678");
        assertThat(account.getProviderAvatarUrl()).isEqualTo(command.identity().avatarUrl());
        assertThat(account.getLastLoginAt()).isEqualTo(user.getLastLoginAt());
    }

    @Test
    @DisplayName("이미 연결된 OAuth 신원으로 중복 가입할 수 없다")
    void rejectsAlreadyLinkedOAuthIdentity() {
        OAuthSignupCommand command = signupCommand();
        given(oauthAccountRepository.findByProviderAndProviderAccountId(
                OAuthProvider.GITHUB,
                "12345678"
        )).willReturn(Optional.of(OAuthAccount.builder()
                .id(10L)
                .userId(1L)
                .provider(OAuthProvider.GITHUB)
                .providerAccountId("12345678")
                .build()));

        assertThatThrownBy(() -> oauthSignupService.signup(command))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    private OAuthSignupCommand signupCommand() {
        return new OAuthSignupCommand(
                "sangjun",
                "상준",
                new OAuthIdentity(
                        OAuthProvider.GITHUB,
                        "12345678",
                        "https://avatars.githubusercontent.com/u/12345678",
                        "https://github.com/sangjun"
                )
        );
    }

    private User savedUser() {
        return User.builder()
                .id(1L)
                .handle("sangjun")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }
}
