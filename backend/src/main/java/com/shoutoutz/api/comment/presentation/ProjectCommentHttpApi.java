package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.ProjectCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentUpdateResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/@{slug}/comments")
@RequiredArgsConstructor
public class ProjectCommentHttpApi {

    private final ProjectCommentService projectCommentService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<ProjectCommentFindResponse.Comment>>> findAll(
            @PathVariable String slug,
            @LoginUser(required = false) AuthenticatedUser loginUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "LATEST") String sort
    ) {
        ProjectCommentFindResponse response = projectCommentService.findAll(
                slug,
                new ProjectCommentFindRequest(cursor, size, sort),
                loginUser == null ? null : loginUser.userId()
        );
        return ResponseEntity.ok(SuccessResponse.success(response.comments(), response.meta()));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<ProjectCommentCreateResponse>> create(
            @PathVariable String slug,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ProjectCommentCreateRequest request
    ) {
        ProjectCommentCreateResponse response = projectCommentService.create(
                slug,
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<ProjectCommentUpdateResponse>> update(
            @PathVariable String slug,
            @PathVariable long commentId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody ProjectCommentUpdateRequest request
    ) {
        ProjectCommentUpdateResponse response = projectCommentService.update(
                slug,
                commentId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<ProjectCommentDeleteResponse>> delete(
            @PathVariable String slug,
            @PathVariable long commentId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        ProjectCommentDeleteResponse response = projectCommentService.delete(
                slug,
                commentId,
                loginUser.userId()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
