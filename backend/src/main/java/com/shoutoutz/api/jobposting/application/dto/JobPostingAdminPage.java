package com.shoutoutz.api.jobposting.application.dto;

import java.util.List;

public record JobPostingAdminPage(List<JobPostingAdminSummary> items, boolean hasNext, long totalCount) {

    public JobPostingAdminPage {
        items = List.copyOf(items);
    }
}
