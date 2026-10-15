package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.InterviewCommentReactionService;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentReactionResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/comments/{commentId}/reactions")
@RequiredArgsConstructor
public class InterviewCommentReactionHttpApi {

    private final InterviewCommentReactionService interviewCommentReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<InterviewCommentReactionResponse>> add(
            @PathVariable long interviewId,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        InterviewCommentReactionResponse response = interviewCommentReactionService.add(
                interviewId,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<InterviewCommentReactionResponse>> remove(
            @PathVariable long interviewId,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        InterviewCommentReactionResponse response = interviewCommentReactionService.remove(
                interviewId,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
