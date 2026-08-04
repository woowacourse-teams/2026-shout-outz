package com.dropit.backend.comment;

import java.util.UUID;

import com.dropit.backend.common.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(name = "Comments", description = "댓글과 답글")
public class CommentController {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    public CommentController(CommentService commentService, CurrentUser currentUser) {
        this.commentService = commentService;
        this.currentUser = currentUser;
    }

    @GetMapping("/apps/{appId}/comments")
    @Operation(summary = "서비스 댓글 목록 조회")
    public CommentListResponse list(@PathVariable String appId) {
        return commentService.list(appId);
    }

    @PostMapping("/apps/{appId}/comments")
    @Operation(summary = "댓글 또는 답글 작성")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CommentResponse> create(
        @PathVariable String appId,
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CommentRequest request
    ) {
        return ResponseEntity.status(201)
            .body(commentService.create(appId, currentUser.requireId(jwt), request));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "내 댓글 삭제")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(
        @PathVariable UUID commentId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        commentService.delete(currentUser.requireId(jwt), commentId);
        return ResponseEntity.noContent().build();
    }
}
