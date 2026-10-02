package com.shoutoutz.api.verification.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.verification.application.AdminVerificationRequestHistoryService;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/verification-requests")
@RequiredArgsConstructor
public class AdminVerificationRequestHistoryHttpApi {

    private final AdminVerificationRequestHistoryService historyService;

    @GetMapping("/{requestId}/history")
    public ResponseEntity<SuccessResponse<AdminVerificationRequestHistoryResponse>> findHistory(
            @PathVariable long requestId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        AdminVerificationRequestHistoryResponse response = historyService.findHistory(
                requestId,
                loginUser.role()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
