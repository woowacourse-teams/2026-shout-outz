package com.shoutoutz.api.interview.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.interview.application.InterviewViewService;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewViewRecordResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import com.shoutoutz.api.visitor.presentation.Visitor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewViewHttpApi {
    private final InterviewViewService service;

    @PostMapping("/{interviewId}/views")
    public SuccessResponse<InterviewViewRecordResponse> record(@PathVariable long interviewId,
            @Visitor VisitorKey visitorKey) {
        return SuccessResponse.success(service.record(interviewId, visitorKey));
    }
}
