package com.shoutoutz.api.resourcereport.application.dto;

import java.util.List;

public record ResourceReportAdminPage(List<ResourceReportAdminItem> items, boolean hasNext, long totalCount) {

    public ResourceReportAdminPage {
        items = List.copyOf(items);
    }
}
