package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.AdminProjectService;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
public class AdminProjectHistoryHttpApi {

    private final AdminProjectService adminProjectService;

    @GetMapping("/{projectId}/history")
    public ResponseEntity<SuccessResponse<AdminProjectHistoryResponse>> findHistory(
            @PathVariable long projectId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                adminProjectService.findHistory(projectId, loginUser.role())
        ));
    }
}
