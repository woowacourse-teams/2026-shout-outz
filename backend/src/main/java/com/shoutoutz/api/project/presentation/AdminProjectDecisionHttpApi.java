package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.AdminProjectService;
import com.shoutoutz.api.project.presentation.dto.request.AdminProjectRejectRequest;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectApproveResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectRejectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
public class AdminProjectDecisionHttpApi {

    private final AdminProjectService adminProjectService;

    @PostMapping("/{projectId}/approve")
    public ResponseEntity<SuccessResponse<AdminProjectApproveResponse>> approve(
            @PathVariable long projectId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                adminProjectService.approve(projectId, loginUser.userId(), loginUser.role())
        ));
    }

    @PostMapping("/{projectId}/reject")
    public ResponseEntity<SuccessResponse<AdminProjectRejectResponse>> reject(
            @PathVariable long projectId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody AdminProjectRejectRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                adminProjectService.reject(
                        projectId,
                        loginUser.userId(),
                        loginUser.role(),
                        request.reason()
                )
        ));
    }
}
