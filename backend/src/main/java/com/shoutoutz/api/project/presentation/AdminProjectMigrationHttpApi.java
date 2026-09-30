package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.AdminProjectMigrationService;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectMigrationUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** 이관 프로젝트의 projects 행을 보정한 뒤 제거할 관리자 전용 API. */
@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
public class AdminProjectMigrationHttpApi {

    private final AdminProjectMigrationService migrationService;

    @PatchMapping("/{projectId}/migration")
    public ResponseEntity<SuccessResponse<AdminProjectMigrationUpdateResponse>> update(
            @PathVariable long projectId,
            @LoginUser AuthenticatedUser loginUser,
            @RequestBody JsonNode fields
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                migrationService.update(projectId, loginUser.userId(), loginUser.role(), fields)
        ));
    }
}
