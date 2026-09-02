package com.shoutoutz.api.auth.presentation.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

class AuthSessionManagerTest {

    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();
    private final AuthSessionManager authSessionManager =
            new AuthSessionManager(authSessionAccessor);

    @Test
    @DisplayName("인증 세션을 수립할 때 Session ID를 회전하고 인증 정보를 저장한다")
    void establishesAuthenticatedSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession();
        String previousSessionId = session.getId();

        authSessionManager.establishAuthenticatedSession(request, 1L, UserRole.USER);

        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        assertThat(authSessionAccessor.findAuthentication(session))
                .contains(new AuthenticatedSession(1L, UserRole.USER));
    }
}
