package com.shoutoutz.api.verification.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.verification.application.UserVerificationService;
import com.shoutoutz.api.verification.presentation.dto.request.UserVerificationRequestCreateRequest;
import com.shoutoutz.api.verification.presentation.dto.response.UserVerificationRequestCreateResponse;
import com.shoutoutz.api.verification.presentation.dto.response.UserVerificationRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserVerificationHttpApi {

    private final UserVerificationService userVerificationService;

    @PostMapping("/verification-requests")
    public ResponseEntity<SuccessResponse<UserVerificationRequestCreateResponse>> create(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody UserVerificationRequestCreateRequest request
    ) {
        UserVerificationRequestCreateResponse response = userVerificationService.create(
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @GetMapping("/verification-request")
    public ResponseEntity<SuccessResponse<UserVerificationRequestResponse>> findLatest(
            @LoginUser AuthenticatedUser loginUser
    ) {
        UserVerificationRequestResponse response = userVerificationService.findLatest(
                loginUser.userId()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
