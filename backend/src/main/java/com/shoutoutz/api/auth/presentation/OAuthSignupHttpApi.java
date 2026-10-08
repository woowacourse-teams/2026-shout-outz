package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.application.OAuthSignupService;
import com.shoutoutz.api.auth.application.command.OAuthSignupResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupHandleAvailabilityRequest;
import com.shoutoutz.api.auth.presentation.dto.request.OAuthSignupRequest;
import com.shoutoutz.api.auth.presentation.dto.response.OAuthSignupHandleAvailabilityResponse;
import com.shoutoutz.api.auth.presentation.dto.response.OAuthSignupResponse;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.auth.presentation.session.AuthSessionManager;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OAuthSignupHttpApi {

    private final OAuthSignupService oauthSignupService;
    private final AuthSessionAccessor authSessionAccessor;
    private final AuthSessionManager authSessionManager;

    @GetMapping("/api/v1/auth/signup/handle-availability")
    public ResponseEntity<SuccessResponse<OAuthSignupHandleAvailabilityResponse>>
    checkHandleAvailability(
            @Valid @ModelAttribute OAuthSignupHandleAvailabilityRequest availabilityRequest,
            HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        authSessionAccessor.findPendingIdentity(session)
                .orElseThrow(() -> new BadRequestException(
                        AuthErrorCode.OAUTH_SIGNUP_SESSION_NOT_FOUND
                ));

        OAuthSignupHandleAvailabilityResponse response =
                new OAuthSignupHandleAvailabilityResponse(
                        oauthSignupService.isHandleAvailable(availabilityRequest.handle())
                );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(SuccessResponse.success(response));
    }

    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<SuccessResponse<OAuthSignupResponse>> signup(
            @Valid @RequestBody OAuthSignupRequest signupRequest,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        OAuthIdentity identity = authSessionAccessor.findPendingIdentity(session)
                .orElseThrow(() -> new BadRequestException(
                        AuthErrorCode.OAUTH_SIGNUP_SESSION_NOT_FOUND
                ));
        OAuthSignupResult result = oauthSignupService.signup(
                signupRequest.toCommand(identity)
        );

        authSessionManager.establishAuthenticatedSession(
                request,
                result.userId(),
                result.role()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(OAuthSignupResponse.from(result)));
    }
}
