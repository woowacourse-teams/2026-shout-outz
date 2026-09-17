package com.shoutoutz.api.verification.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.verification.application.AdminVerificationRequestService;
import com.shoutoutz.api.verification.presentation.dto.request.AdminVerificationRequestFindAllRequest;
import com.shoutoutz.api.verification.presentation.dto.response.AdminVerificationRequestFindAllResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/verification-requests")
@RequiredArgsConstructor
public class AdminVerificationRequestHttpApi {

    private final AdminVerificationRequestService adminVerificationRequestService;

    @GetMapping
    public ResponseEntity<SuccessResponse<AdminVerificationRequestFindAllResponse>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @ModelAttribute AdminVerificationRequestFindAllRequest request
    ) {
        AdminVerificationRequestFindAllResponse response = adminVerificationRequestService.findAll(
                loginUser.role(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
