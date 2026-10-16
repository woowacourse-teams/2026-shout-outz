package com.shoutoutz.api.interview.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.interview.application.InterviewService;
import com.shoutoutz.api.interview.application.dto.InterviewPage;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewDetailResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import com.shoutoutz.api.visitor.presentation.Visitor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewHttpApi {

    private final InterviewService interviewService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<InterviewSummaryResponse>>> findAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @LoginUser(required = false) AuthenticatedUser loginUser
    ) {
        InterviewPage page = interviewService.findAll(cursor, size, AuthenticatedUser.userIdOrNull(loginUser));
        return ResponseEntity.ok(SuccessResponse.success(page.items(), page.meta()));
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<SuccessResponse<InterviewDetailResponse>> find(
            @PathVariable long interviewId,
            @LoginUser(required = false) AuthenticatedUser loginUser,
            @Visitor VisitorKey visitorKey
    ) {
        InterviewDetailResponse response = interviewService.find(
                interviewId, AuthenticatedUser.userIdOrNull(loginUser), visitorKey);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
