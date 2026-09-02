package com.shoutoutz.api.auth.presentation.session;

import com.shoutoutz.api.user.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthSessionManager {

    private final AuthSessionAccessor authSessionAccessor;

    public void establishAuthenticatedSession(
            HttpServletRequest request,
            Long userId,
            UserRole role
    ) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("인증 세션이 없습니다.");
        }

        request.changeSessionId();
        authSessionAccessor.saveAuthentication(session, userId, role);
    }
}
