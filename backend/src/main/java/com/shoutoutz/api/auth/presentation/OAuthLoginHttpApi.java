package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.application.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.application.OAuthLoginService;
import com.shoutoutz.api.auth.application.OAuthLoginStartResult;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
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

    private final OAuthLoginService oauthLoginService;
    private final OAuthLoginProperties properties;
    private final AuthSessionAccessor authSessionAccessor;

    @GetMapping("/oauth2/authorization/github")
    public ResponseEntity<Void> authorizeGitHub(HttpSession session) {
        OAuthLoginStartResult result = oauthLoginService.startGitHubLogin();
        authSessionAccessor.saveLoginAttempt(session, result.attempt());

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
        OAuthLoginAttempt attempt = authSessionAccessor.consumeLoginAttempt(session);
        OAuthLoginCallbackResult result = oauthLoginService.completeGitHubLogin(
                code,
                state,
                attempt
        );

        if (result.status() == OAuthLoginCallbackResult.Status.AUTHENTICATED) {
            request.changeSessionId();
            authSessionAccessor.saveAuthentication(session, result.userId(), result.role());
        } else {
            authSessionAccessor.savePendingIdentity(session, result.identity());
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(properties.completionUri())
                .build();
    }
}
