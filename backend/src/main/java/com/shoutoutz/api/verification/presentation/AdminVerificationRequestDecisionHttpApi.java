package com.shoutoutz.api.verification.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.verification.application.AdminVerificationRequestDecisionService;
import com.shoutoutz.api.verification.presentation.dto.request.AdminVerificationRequestRejectRequest;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestApproveResponse;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestRejectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/verification-requests")
@RequiredArgsConstructor
public class AdminVerificationRequestDecisionHttpApi {

    private final AdminVerificationRequestDecisionService decisionService;

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<SuccessResponse<AdminVerificationRequestApproveResponse>> approve(
            @PathVariable long requestId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        AdminVerificationRequestApproveResponse response = decisionService.approve(
                requestId,
                loginUser.userId(),
                loginUser.role()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<SuccessResponse<AdminVerificationRequestRejectResponse>> reject(
            @PathVariable long requestId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody AdminVerificationRequestRejectRequest request
    ) {
        AdminVerificationRequestRejectResponse response = decisionService.reject(
                requestId,
                loginUser.userId(),
                loginUser.role(),
                request.reason()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
