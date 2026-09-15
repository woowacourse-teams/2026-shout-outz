package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.FeedCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentUpdateResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds/{feedId}/comments")
@RequiredArgsConstructor
public class FeedCommentHttpApi {

    private final FeedCommentService feedCommentService;

    @PostMapping
    public ResponseEntity<SuccessResponse<FeedCommentCreateResponse>> create(
            @PathVariable long feedId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody FeedCommentCreateRequest request
    ) {
        FeedCommentCreateResponse response = feedCommentService.create(
                feedId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<FeedCommentUpdateResponse>> update(
            @PathVariable long feedId,
            @PathVariable long commentId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody FeedCommentUpdateRequest request
    ) {
        FeedCommentUpdateResponse response = feedCommentService.update(
                feedId,
                commentId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
