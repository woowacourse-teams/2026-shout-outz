package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.application.LoadTestLoginService;
import com.shoutoutz.api.auth.presentation.dto.request.LoadTestLoginRequest;
import com.shoutoutz.api.auth.presentation.dto.response.AuthSessionResponse;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.user.domain.account.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * [부하 테스트 전용] GitHub OAuth 없이 handle만으로 로그인 세션을 만든다.
 * loadtest 프로필에서만 등록되고 prod에서는 등록되지 않는다.
 */
@Profile("loadtest & !prod")
@RestController
@RequiredArgsConstructor
public class LoadTestLoginHttpApi {

    private final LoadTestLoginService loadTestLoginService;
    private final AuthSessionManager authSessionManager;
    private final CsrfTokenManager csrfTokenManager;

    @PostMapping("/loadtest/login")
    public ResponseEntity<SuccessResponse<AuthSessionResponse>> login(
            @Valid @RequestBody LoadTestLoginRequest loginRequest,
            HttpServletRequest request
    ) {
        User user = loadTestLoginService.findLoginUser(loginRequest.handle());

        // 세션 id를 바꾸는 establishAuthenticatedSession은 기존 세션이 있어야 하므로 먼저 만든다.
        request.getSession(true);
        authSessionManager.establishAuthenticatedSession(request, user.getId(), user.getRole());
        String csrfToken = csrfTokenManager.getOrCreate(request.getSession(false));

        AuthSessionResponse response = AuthSessionResponse.authenticated(
                new AuthenticatedSession(user.getId(), user.getRole()),
                csrfToken
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(SuccessResponse.success(response));
    }
}
