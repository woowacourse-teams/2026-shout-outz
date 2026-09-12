package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.presentation.dto.response.AuthSessionResponse;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
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
    public ResponseEntity<SuccessResponse<AuthSessionResponse>> getAuthSession(
            HttpSession session
    ) {
        String csrfToken = csrfTokenManager.getOrCreate(session);
        Optional<AuthenticatedSession> authentication =
                authSessionAccessor.findAuthentication(session);
        AuthSessionResponse response;
        if (authentication.isPresent()) {
            response = AuthSessionResponse.authenticated(authentication.get(), csrfToken);
        } else if (authSessionAccessor.findPendingIdentity(session).isPresent()) {
            response = AuthSessionResponse.signupRequired(csrfToken);
        } else {
            response = AuthSessionResponse.unauthenticated(csrfToken);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(SuccessResponse.success(response));
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authSessionManager.invalidateSession(request);
        return ResponseEntity.noContent().build();
    }
}
