package com.shoutoutz.api.jobposting.application.dto;

import java.util.List;

public record JobPostingPage(List<JobPostingSummary> items, boolean hasNext, long totalCount) {

    public JobPostingPage {
        items = List.copyOf(items);
    }
}
