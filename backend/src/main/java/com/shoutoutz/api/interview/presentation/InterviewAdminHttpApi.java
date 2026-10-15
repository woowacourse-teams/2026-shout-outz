package com.shoutoutz.api.interview.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.interview.application.InterviewService;
import com.shoutoutz.api.interview.application.dto.InterviewPage;
import com.shoutoutz.api.interview.presentation.dto.request.InterviewSaveRequest;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewAdminResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/interviews")
@RequiredArgsConstructor
public class InterviewAdminHttpApi {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<SuccessResponse<InterviewAdminResponse>> create(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody InterviewSaveRequest request
    ) {
        InterviewAdminResponse response = interviewService.create(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<InterviewSummaryResponse>>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        InterviewPage page = interviewService.findAllAdmin(loginUser.userId(), cursor, size);
        return ResponseEntity.ok(SuccessResponse.success(page.items(), page.meta()));
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<SuccessResponse<InterviewAdminResponse>> find(
            @PathVariable long interviewId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        InterviewAdminResponse response = interviewService.findAdmin(interviewId, loginUser.userId());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PutMapping("/{interviewId}")
    public ResponseEntity<SuccessResponse<InterviewAdminResponse>> update(
            @PathVariable long interviewId,
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody InterviewSaveRequest request
    ) {
        InterviewAdminResponse response = interviewService.update(interviewId, loginUser.userId(), request);
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping("/{interviewId}/publish")
    public ResponseEntity<SuccessResponse<InterviewAdminResponse>> publish(
            @PathVariable long interviewId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        InterviewAdminResponse response = interviewService.publish(interviewId, loginUser.userId());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping("/{interviewId}/unpublish")
    public ResponseEntity<SuccessResponse<InterviewAdminResponse>> unpublish(
            @PathVariable long interviewId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        InterviewAdminResponse response = interviewService.unpublish(interviewId, loginUser.userId());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{interviewId}")
    public ResponseEntity<Void> delete(
            @PathVariable long interviewId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        interviewService.delete(interviewId, loginUser.userId());
        return ResponseEntity.noContent().build();
    }
}
