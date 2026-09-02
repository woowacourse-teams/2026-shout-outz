package com.shoutoutz.api.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountEntity;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountJpaRepository;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OAuthAccountRepositoryIntegrationTest {

    private static final Instant AUTHENTICATED_AT = Instant.parse("2026-09-02T00:00:00Z");

    @Autowired
    private OAuthAccountRepository oauthAccountRepository;

    @Autowired
    private OAuthAccountJpaRepository oauthAccountJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("OAuth 계정을 저장하고 Provider와 Provider 계정 ID로 조회한다")
    void savesAndFindsOAuthAccountByProviderIdentity() {
        User user = userRepository.save(User.initialize("sangjun-oauth"));
        OAuthAccount account = OAuthAccount.initialize(
                user.getId(),
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678",
                AUTHENTICATED_AT
        );

        OAuthAccount savedAccount = oauthAccountRepository.save(account);
        OAuthAccount foundAccount = oauthAccountRepository.findByProviderAndProviderAccountId(
                OAuthProvider.GITHUB,
                "12345678"
        ).orElseThrow();
        OAuthAccountEntity savedEntity = oauthAccountJpaRepository.findById(savedAccount.getId())
                .orElseThrow();

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(foundAccount.getId()).isEqualTo(savedAccount.getId());
        assertThat(foundAccount.getUserId()).isEqualTo(user.getId());
        assertThat(foundAccount.getProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(foundAccount.getProviderAccountId()).isEqualTo("12345678");
        assertThat(foundAccount.getLastSyncedAt()).isEqualTo(AUTHENTICATED_AT);
        assertThat(foundAccount.getLastLoginAt()).isEqualTo(AUTHENTICATED_AT);
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("연결되지 않은 Provider 계정은 빈 결과를 반환한다")
    void returnsEmptyWhenProviderIdentityDoesNotExist() {
        assertThat(oauthAccountRepository.findByProviderAndProviderAccountId(
                OAuthProvider.GITHUB,
                "99999999"
        )).isEmpty();
    }

    @Test
    @DisplayName("같은 Provider 계정은 여러 사용자에게 연결할 수 없다")
    void rejectsDuplicateProviderIdentity() {
        User dahye = userRepository.save(User.initialize("dahye-oauth"));
        User sangjun = userRepository.save(User.initialize("sangjun-oauth"));
        oauthAccountRepository.save(OAuthAccount.initialize(
                dahye.getId(),
                OAuthProvider.GITHUB,
                "12345678",
                null,
                AUTHENTICATED_AT
        ));

        assertThatThrownBy(() -> {
            oauthAccountRepository.save(OAuthAccount.initialize(
                    sangjun.getId(),
                    OAuthProvider.GITHUB,
                    "12345678",
                    null,
                    AUTHENTICATED_AT
            ));
            oauthAccountJpaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 OAuth 계정을 연결할 수 없다")
    void rejectsNonexistentUserId() {
        assertThatThrownBy(() -> {
            oauthAccountRepository.save(OAuthAccount.initialize(
                    Long.MAX_VALUE,
                    OAuthProvider.GITHUB,
                    "12345678",
                    null,
                    AUTHENTICATED_AT
            ));
            oauthAccountJpaRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
