package com.shoutoutz.api.bugreport.application.dto;

import com.shoutoutz.api.bugreport.domain.BugReport;
import java.util.List;

public record BugReportPage(List<BugReport> items, boolean hasNext, long totalCount) {

    public BugReportPage {
        items = List.copyOf(items);
    }
}
