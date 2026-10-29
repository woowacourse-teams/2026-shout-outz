package com.shoutoutz.api.jobposting.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.jobposting.application.JobPostingAdminService;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingExtractRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.request.AdminJobPostingSaveRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingExtractResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.AdminJobPostingSaveResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/job-postings")
@RequiredArgsConstructor
public class AdminJobPostingHttpApi {

    private final JobPostingAdminService jobPostingAdminService;

    @PostMapping("/extractions")
    public ResponseEntity<SuccessResponse<AdminJobPostingExtractResponse>> extract(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody AdminJobPostingExtractRequest request
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                jobPostingAdminService.extract(loginUser.role(), request)
        ));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<AdminJobPostingSaveResponse>> register(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @RequestBody AdminJobPostingSaveRequest request
    ) {
        AdminJobPostingSaveResponse response = jobPostingAdminService.register(
                loginUser.userId(),
                loginUser.role(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<AdminJobPostingFindAllResponse.Item>>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @Valid @ModelAttribute AdminJobPostingFindAllRequest request
    ) {
        AdminJobPostingFindAllResponse response = jobPostingAdminService.findAll(
                loginUser.role(),
                request
        );
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    @GetMapping("/{jobPostingId}")
    public ResponseEntity<SuccessResponse<AdminJobPostingDetailResponse>> findDetail(
            @PathVariable long jobPostingId,
            @LoginUser AuthenticatedUser loginUser
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                jobPostingAdminService.findDetail(jobPostingId, loginUser.role())
        ));
    }
}
