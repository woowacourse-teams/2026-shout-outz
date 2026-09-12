package com.shoutoutz.api.auth.presentation.session;

import com.shoutoutz.api.user.domain.account.UserRole;
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
        HttpSession session = rotateSessionId(request);
        authSessionAccessor.saveAuthentication(session, userId, role);
    }

    public HttpSession rotateSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("인증 세션이 없습니다.");
        }

        request.changeSessionId();
        return session;
    }

    public void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
