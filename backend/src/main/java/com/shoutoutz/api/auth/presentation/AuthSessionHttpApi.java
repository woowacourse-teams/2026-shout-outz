package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.presentation.dto.response.AuthSessionResponse;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthSessionHttpApi {

    private final CsrfTokenManager csrfTokenManager;
    private final AuthSessionAccessor authSessionAccessor;
    private final AuthSessionManager authSessionManager;

    @GetMapping("/api/v1/auth/session")
    public AuthSessionResponse getAuthSession(HttpSession session) {
        String csrfToken = csrfTokenManager.getOrCreate(session);
        Optional<AuthenticatedSession> authentication =
                authSessionAccessor.findAuthentication(session);
        if (authentication.isPresent()) {
            return AuthSessionResponse.authenticated(authentication.get(), csrfToken);
        }

        if (authSessionAccessor.findPendingIdentity(session).isPresent()) {
            return AuthSessionResponse.signupRequired(csrfToken);
        }

        return AuthSessionResponse.unauthenticated(csrfToken);
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authSessionManager.invalidateSession(request);
        return ResponseEntity.noContent().build();
    }
}
