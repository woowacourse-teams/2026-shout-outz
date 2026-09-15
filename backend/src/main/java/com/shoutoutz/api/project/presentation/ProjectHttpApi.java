package com.shoutoutz.api.project.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.project.application.ProjectService;
import com.shoutoutz.api.project.presentation.dto.request.ProjectCreateRequest;
import com.shoutoutz.api.project.presentation.dto.request.ProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.response.ProjectCreateResponse;
import com.shoutoutz.api.project.presentation.dto.response.ProjectFindAllResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectHttpApi {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<SuccessResponse<ProjectCreateResponse>> create(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        ProjectCreateResponse response = projectService.create(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<ProjectFindAllResponse.Item>>> findAll(
            @Valid @ModelAttribute ProjectFindAllRequest request
    ) {
        ProjectFindAllResponse response = projectService.findAll(request);
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }
}
