package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.ProjectCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/comments")
@RequiredArgsConstructor
public class ProjectCommentHttpApi {

    private final ProjectCommentService projectCommentService;

    @PostMapping
    public ResponseEntity<SuccessResponse<ProjectCommentCreateResponse>> create(
            @PathVariable long projectId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ProjectCommentCreateRequest request
    ) {
        ProjectCommentCreateResponse response = projectCommentService.create(
                projectId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }
}
