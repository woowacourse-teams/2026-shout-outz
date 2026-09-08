package com.shoutoutz.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.auth.application.dto.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountJpaRepository;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.infrastructure.jpa.UserJpaRepository;
import com.shoutoutz.api.user.infrastructure.jpa.UserProfileJpaRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OAuthSignupServiceIntegrationTest {

    @Autowired
    private OAuthSignupService oauthSignupService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserProfileJpaRepository userProfileJpaRepository;

    @Autowired
    private OAuthAccountJpaRepository oauthAccountJpaRepository;

    @Test
    @DisplayName("프로필 생성에 실패하면 사용자와 OAuth 계정 생성을 모두 롤백한다")
    void rollsBackSignupWhenProfileCreationFails() {
        long userCount = userJpaRepository.count();
        long profileCount = userProfileJpaRepository.count();
        long oauthAccountCount = oauthAccountJpaRepository.count();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OAuthSignupCommand invalidCommand = new OAuthSignupCommand(
                "dahye-" + suffix,
                "다혜",
                UserType.WOOWACOURSE_CREW,
                null,
                null,
                new OAuthIdentity(OAuthProvider.GITHUB, suffix, null)
        );

        assertThatThrownBy(() -> oauthSignupService.signup(invalidCommand))
                .isInstanceOf(DomainValidationException.class);

        assertThat(userJpaRepository.count()).isEqualTo(userCount);
        assertThat(userProfileJpaRepository.count()).isEqualTo(profileCount);
        assertThat(oauthAccountJpaRepository.count()).isEqualTo(oauthAccountCount);
    }
}
