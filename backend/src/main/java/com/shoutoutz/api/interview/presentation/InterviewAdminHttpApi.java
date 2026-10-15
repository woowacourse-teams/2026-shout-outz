package com.shoutoutz.api.interview.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.application.InterviewService;
import com.shoutoutz.api.interview.application.InterviewQueryService;
import com.shoutoutz.api.interview.application.dto.InterviewPage;
import com.shoutoutz.api.interview.presentation.dto.request.InterviewSaveRequest;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewAdminResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/interviews")
@RequiredArgsConstructor
public class InterviewAdminHttpApi {
    private final InterviewService service;
    private final InterviewQueryService queryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<InterviewAdminResponse> create(
            @LoginUser AuthenticatedUser user, @Valid @RequestBody InterviewSaveRequest request) {
        Interview interview = service.create(user.userId(), request);
        return SuccessResponse.success(queryService.findAdmin(interview.getId(), user.userId()));
    }

    @GetMapping
    public SuccessResponse<List<InterviewSummaryResponse>> findAll(
            @LoginUser AuthenticatedUser user, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        InterviewPage page = queryService.findAllAdmin(user.userId(), cursor, size);
        return SuccessResponse.success(page.items(), page.meta());
    }

    @GetMapping("/{interviewId}")
    public SuccessResponse<InterviewAdminResponse> find(@PathVariable long interviewId,
            @LoginUser AuthenticatedUser user) {
        return SuccessResponse.success(queryService.findAdmin(interviewId, user.userId()));
    }

    @PutMapping("/{interviewId}")
    public SuccessResponse<InterviewAdminResponse> update(@PathVariable long interviewId,
            @LoginUser AuthenticatedUser user, @Valid @RequestBody InterviewSaveRequest request) {
        Interview interview = service.update(interviewId, user.userId(), request);
        return SuccessResponse.success(queryService.findAdmin(interview.getId(), user.userId()));
    }

    @PostMapping("/{interviewId}/publish")
    public SuccessResponse<InterviewAdminResponse> publish(@PathVariable long interviewId,
            @LoginUser AuthenticatedUser user) {
        Interview interview = service.publish(interviewId, user.userId());
        return SuccessResponse.success(queryService.findAdmin(interview.getId(), user.userId()));
    }

    @PostMapping("/{interviewId}/unpublish")
    public SuccessResponse<InterviewAdminResponse> unpublish(@PathVariable long interviewId,
            @LoginUser AuthenticatedUser user) {
        Interview interview = service.unpublish(interviewId, user.userId());
        return SuccessResponse.success(queryService.findAdmin(interview.getId(), user.userId()));
    }

    @DeleteMapping("/{interviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long interviewId, @LoginUser AuthenticatedUser user) {
        service.delete(interviewId, user.userId());
    }
}
