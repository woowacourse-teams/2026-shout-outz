package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.ProjectCommentReactionService;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentReactionResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/@{slug}/comments/{commentId}/reactions")
@RequiredArgsConstructor
public class ProjectCommentReactionHttpApi {

    private final ProjectCommentReactionService projectCommentReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<ProjectCommentReactionResponse>> add(
            @PathVariable String slug,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        ProjectCommentReactionResponse response = projectCommentReactionService.add(
                slug,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<ProjectCommentReactionResponse>> remove(
            @PathVariable String slug,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        ProjectCommentReactionResponse response = projectCommentReactionService.remove(
                slug,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
