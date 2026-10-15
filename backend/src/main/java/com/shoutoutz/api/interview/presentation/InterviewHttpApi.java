package com.shoutoutz.api.interview.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.interview.application.InterviewQueryService;
import com.shoutoutz.api.interview.application.dto.InterviewPage;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewDetailResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
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
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size) {
        InterviewPage page = queryService.findAll(cursor, size);
        return SuccessResponse.success(page.items(), page.meta());
    }

    @GetMapping("/{interviewId}")
    public SuccessResponse<InterviewDetailResponse> find(@PathVariable long interviewId) {
        return SuccessResponse.success(queryService.find(interviewId));
    }
}
