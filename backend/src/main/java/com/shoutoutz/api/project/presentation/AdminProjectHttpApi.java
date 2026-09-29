package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.AdminProjectService;
import com.shoutoutz.api.project.presentation.dto.request.AdminProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectDetailResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectFindAllResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
public class AdminProjectHttpApi {

    private final AdminProjectService adminProjectService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<AdminProjectFindAllResponse.Item>>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @ModelAttribute AdminProjectFindAllRequest request
    ) {
        AdminProjectFindAllResponse response = adminProjectService.findAll(loginUser.role(), request);
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<SuccessResponse<AdminProjectDetailResponse>> findDetail(
            @PathVariable long projectId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                adminProjectService.findDetail(projectId, loginUser.role())
        ));
    }
}
