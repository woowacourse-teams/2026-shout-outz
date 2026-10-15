package com.shoutoutz.api.interview.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.interview.application.InterviewQueryService;
import com.shoutoutz.api.interview.application.dto.InterviewPage;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewDetailResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import com.shoutoutz.api.visitor.presentation.Visitor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewHttpApi {
    private final InterviewQueryService queryService;

    @GetMapping
    public SuccessResponse<List<InterviewSummaryResponse>> findAll(
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size,
            @LoginUser(required = false) AuthenticatedUser user) {
        InterviewPage page = queryService.findAll(cursor, size, AuthenticatedUser.userIdOrNull(user));
        return SuccessResponse.success(page.items(), page.meta());
    }

    @GetMapping("/{interviewId}")
    public SuccessResponse<InterviewDetailResponse> find(@PathVariable long interviewId,
            @LoginUser(required = false) AuthenticatedUser user, @Visitor VisitorKey visitorKey) {
        return SuccessResponse.success(queryService.find(
                interviewId, AuthenticatedUser.userIdOrNull(user), visitorKey));
    }
}
