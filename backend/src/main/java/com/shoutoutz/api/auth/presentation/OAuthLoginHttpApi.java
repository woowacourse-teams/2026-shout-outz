package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.application.OAuthLoginService;
import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.application.command.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OAuthLoginHttpApi {

    private final OAuthLoginService oauthLoginService;
    private final OAuthLoginProperties properties;
    private final AuthSessionAccessor authSessionAccessor;
    private final AuthSessionManager authSessionManager;

    @GetMapping("/oauth2/authorization/github")
    public ResponseEntity<Void> authorizeGitHub(
            @RequestParam(name = "returnTo", required = false) URI returnTo,
            HttpSession session
    ) {
        URI completionUri = properties.resolveCompletionUri(returnTo);
        OAuthLoginAttempt attempt = authSessionAccessor.getOrCreateLoginAttempt(session, completionUri);
        URI authorizationUri = oauthLoginService.generateGitHubAuthorizationUri(attempt);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authorizationUri)
                .build();
    }

    @GetMapping(
            value = "/login/oauth2/code/github",
            params = {"code", "!error"}
    )
    public ResponseEntity<Void> callbackGitHub(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        OAuthLoginAttempt attempt = authSessionAccessor.beginCallbackProcessing(session, state);
        try {
            OAuthLoginCallbackResult result = oauthLoginService.completeGitHubLogin(
                    code,
                    state,
                    attempt
            );
            authSessionManager.completeOAuthLogin(request, attempt, result);
            return redirectToCompletion(attempt.completionUri());
        } finally {
            authSessionAccessor.finishLoginAttempt(session, attempt);
        }
    }

    @GetMapping(
            value = "/login/oauth2/code/github",
            params = {"error=access_denied", "!code"}
    )
    public ResponseEntity<Void> handleDeniedGitHubAuthorization(
            @RequestParam String state,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        OAuthLoginAttempt attempt = authSessionAccessor.beginCallbackProcessing(session, state);
        try {
            oauthLoginService.validateGitHubCallback(state, attempt);
            return redirectToCompletion(attempt.completionUri());
        } finally {
            authSessionAccessor.finishLoginAttempt(session, attempt);
        }
    }

    private ResponseEntity<Void> redirectToCompletion(URI completionUri) {
        URI resolvedCompletionUri = properties.completionUri();
        if (completionUri != null) {
            resolvedCompletionUri = properties.resolveCompletionUri(completionUri);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(resolvedCompletionUri)
                .build();
    }
}
