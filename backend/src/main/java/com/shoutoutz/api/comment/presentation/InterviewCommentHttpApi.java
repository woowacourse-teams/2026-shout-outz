package com.shoutoutz.api.comment.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.comment.application.InterviewCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentUpdateResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/comments")
@RequiredArgsConstructor
public class InterviewCommentHttpApi {

    private final InterviewCommentService interviewCommentService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<InterviewCommentFindResponse.Comment>>> findAll(
            @PathVariable long interviewId,
            @LoginUser(required = false) AuthenticatedUser loginUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "LATEST") String sort
    ) {
        InterviewCommentFindResponse response = interviewCommentService.findAll(
                interviewId,
                new InterviewCommentFindRequest(cursor, size, sort),
                AuthenticatedUser.userIdOrNull(loginUser)
        );
        return ResponseEntity.ok(SuccessResponse.success(response.comments(), response.meta()));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<InterviewCommentCreateResponse>> create(
            @PathVariable long interviewId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody InterviewCommentCreateRequest request
    ) {
        InterviewCommentCreateResponse response = interviewCommentService.create(
                interviewId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<InterviewCommentUpdateResponse>> update(
            @PathVariable long interviewId,
            @PathVariable long commentId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody InterviewCommentUpdateRequest request
    ) {
        InterviewCommentUpdateResponse response = interviewCommentService.update(
                interviewId,
                commentId,
                loginUser.userId(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<SuccessResponse<InterviewCommentDeleteResponse>> delete(
            @PathVariable long interviewId,
            @PathVariable long commentId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        InterviewCommentDeleteResponse response = interviewCommentService.delete(
                interviewId,
                commentId,
                loginUser.userId()
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
