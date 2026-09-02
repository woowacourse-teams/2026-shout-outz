package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.application.OAuthLoginService;
import com.shoutoutz.api.auth.application.OAuthLoginStartResult;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2/authorization")
@RequiredArgsConstructor
public class OAuthLoginHttpApi {

    private final OAuthLoginService oauthLoginService;

    @GetMapping("/github")
    public ResponseEntity<Void> authorizeGitHub(HttpSession session) {
        OAuthLoginStartResult result = oauthLoginService.startGitHubLogin();
        session.setAttribute("oauthLoginAttempt", result.attempt());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(result.authorizationUri())
                .build();
    }
}
