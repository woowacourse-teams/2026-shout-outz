package com.shoutoutz.api.jobposting.application.dto;

import java.util.List;

public record JobPostingListPage(List<JobPostingListItem> items, boolean hasNext) {

    public JobPostingListPage {
        items = List.copyOf(items);
    }
}
