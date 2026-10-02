package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.FeedCommentReactionService;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentReactionResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds/{feedId}/comments/{commentId}/reactions")
@RequiredArgsConstructor
public class FeedCommentReactionHttpApi {

    private final FeedCommentReactionService feedCommentReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<FeedCommentReactionResponse>> add(
            @PathVariable long feedId,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        FeedCommentReactionResponse response = feedCommentReactionService.add(
                feedId,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<FeedCommentReactionResponse>> remove(
            @PathVariable long feedId,
            @PathVariable long commentId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        FeedCommentReactionResponse response = feedCommentReactionService.remove(
                feedId,
                commentId,
                user.userId(),
                type
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
