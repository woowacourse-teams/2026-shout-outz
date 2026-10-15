package com.shoutoutz.api.interview.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.interview.application.InterviewReactionService;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interview/{interviewId}/reactions")
@RequiredArgsConstructor
public class InterviewReactionHttpApi {

    private final InterviewReactionService interviewReactionService;

    @PutMapping("/{type}")
    public ResponseEntity<SuccessResponse<InterviewReactionResponse>> add(
            @PathVariable long interviewId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        InterviewReactionResponse response = interviewReactionService.add(interviewId, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<SuccessResponse<InterviewReactionResponse>> remove(
            @PathVariable long interviewId,
            @PathVariable String type,
            @LoginUser AuthenticatedUser user
    ) {
        InterviewReactionResponse response = interviewReactionService.remove(interviewId, user.userId(), type);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
