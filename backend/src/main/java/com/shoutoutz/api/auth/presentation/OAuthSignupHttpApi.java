package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.application.OAuthSignupService;
import com.shoutoutz.api.auth.application.dto.result.OAuthSignupResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupRequest;
import com.shoutoutz.api.auth.presentation.dto.response.OAuthSignupResponse;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OAuthSignupHttpApi {

    private final OAuthSignupService oauthSignupService;
    private final AuthSessionAccessor authSessionAccessor;
    private final AuthSessionManager authSessionManager;

    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<OAuthSignupResponse> signup(
            @Valid @RequestBody OAuthSignupRequest signupRequest,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        OAuthIdentity identity = authSessionAccessor.findPendingIdentity(session)
                .orElseThrow(() -> new IllegalStateException("가입 대기 OAuth 신원이 없습니다."));
        OAuthSignupResult result = oauthSignupService.signup(
                signupRequest.toCommand(identity)
        );

        authSessionManager.establishAuthenticatedSession(
                request,
                result.userId(),
                result.role()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OAuthSignupResponse.from(result));
    }
}
