package com.shoutoutz.api.interview.application.dto;

import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewSummaryResponse;
import java.util.List;

public record InterviewPage(List<InterviewSummaryResponse> items, SliceMetaResponse meta) {
}
