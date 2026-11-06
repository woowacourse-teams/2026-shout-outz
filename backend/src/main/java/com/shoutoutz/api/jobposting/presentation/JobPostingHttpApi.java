package com.shoutoutz.api.jobposting.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.jobposting.application.JobPostingService;
import com.shoutoutz.api.jobposting.presentation.dto.request.JobPostingFindAllRequest;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingDetailResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingFindAllResponse;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobRequirementRecommendationResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 채용공고 조회. 로그인 없이 볼 수 있다.
 */
@RestController
@RequestMapping("/api/v1/job-postings")
@RequiredArgsConstructor
public class JobPostingHttpApi {

    private final JobPostingService jobPostingService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<JobPostingFindAllResponse.Item>>> findAll(
            @Valid @ModelAttribute JobPostingFindAllRequest request
    ) {
        JobPostingFindAllResponse response = jobPostingService.findAll(request);
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    @GetMapping("/{jobPostingId}")
    public ResponseEntity<SuccessResponse<JobPostingDetailResponse>> findDetail(@PathVariable long jobPostingId) {
        return ResponseEntity.ok(SuccessResponse.success(jobPostingService.findDetail(jobPostingId)));
    }

    @GetMapping("/{jobPostingId}/requirements/{requirementId}/recommendations")
    public ResponseEntity<SuccessResponse<List<JobRequirementRecommendationResponse>>> findRecommendations(
            @PathVariable long jobPostingId,
            @PathVariable long requirementId
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                jobPostingService.findRecommendations(jobPostingId, requirementId)));
    }
}
