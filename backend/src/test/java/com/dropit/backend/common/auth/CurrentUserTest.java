package com.dropit.backend.common.auth;

import java.util.UUID;

import com.dropit.backend.common.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @Test
    void extractsUuidFromJwtSubject() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(userId.toString())
            .build();

        assertThat(currentUser.requireId(jwt)).isEqualTo(userId);
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThatThrownBy(() -> currentUser.requireId(null))
            .isInstanceOf(ApiException.class)
            .hasMessage("로그인이 필요합니다.");
    }
}
