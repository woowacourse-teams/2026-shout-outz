package com.shoutoutz.api.bugreport.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.bugreport.application.BugReportService;
import com.shoutoutz.api.bugreport.presentation.dto.request.BugReportCreateRequest;
import com.shoutoutz.api.bugreport.presentation.dto.response.BugReportCreateResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bug-reports")
@RequiredArgsConstructor
public class BugReportHttpApi {

    private final BugReportService bugReportService;

    @PostMapping
    public ResponseEntity<SuccessResponse<BugReportCreateResponse>> create(
            @LoginUser(required = false) AuthenticatedUser loginUser,
            @Valid @RequestBody BugReportCreateRequest request
    ) {
        BugReportCreateResponse response = bugReportService.create(
                AuthenticatedUser.userIdOrNull(loginUser),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }
}
