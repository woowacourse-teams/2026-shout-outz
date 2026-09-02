package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.application.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.application.OAuthLoginService;
import com.shoutoutz.api.auth.application.OAuthLoginStartResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OAuthLoginHttpApi {

    private static final String LOGIN_ATTEMPT = "oauthLoginAttempt";
    private static final String AUTHENTICATED_SESSION = "authenticatedSession";
    private static final String PENDING_OAUTH_IDENTITY = "pendingOAuthIdentity";

    private final OAuthLoginService oauthLoginService;
    private final OAuthLoginProperties properties;

    @GetMapping("/oauth2/authorization/github")
    public ResponseEntity<Void> authorizeGitHub(HttpSession session) {
        OAuthLoginStartResult result = oauthLoginService.startGitHubLogin();
        session.setAttribute(LOGIN_ATTEMPT, result.attempt());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(result.authorizationUri())
                .build();
    }

    @GetMapping("/login/oauth2/code/github")
    public ResponseEntity<Void> callbackGitHub(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        OAuthLoginAttempt attempt = getAndRemoveLoginAttempt(session);
        OAuthLoginCallbackResult result = oauthLoginService.completeGitHubLogin(
                code,
                state,
                attempt
        );

        if (result.status() == OAuthLoginCallbackResult.Status.AUTHENTICATED) {
            request.changeSessionId();
            session.setAttribute(
                    AUTHENTICATED_SESSION,
                    new AuthenticatedSession(result.userId(), result.role())
            );
            session.removeAttribute(PENDING_OAUTH_IDENTITY);
        } else {
            session.setAttribute(PENDING_OAUTH_IDENTITY, result.identity());
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(properties.completionUri())
                .build();
    }

    private OAuthLoginAttempt getAndRemoveLoginAttempt(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("OAuth 로그인 세션이 없습니다.");
        }
        Object value = session.getAttribute(LOGIN_ATTEMPT);
        session.removeAttribute(LOGIN_ATTEMPT);
        if (!(value instanceof OAuthLoginAttempt attempt)) {
            throw new IllegalArgumentException("OAuth 로그인 시도가 없습니다.");
        }
        return attempt;
    }
}
