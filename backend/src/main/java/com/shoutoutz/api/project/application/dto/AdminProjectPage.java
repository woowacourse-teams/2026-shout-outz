package com.shoutoutz.api.project.application.dto;

import java.util.List;

public record AdminProjectPage(
        List<AdminProjectItem> items,
        boolean hasNext,
        long totalCount
) {

    public AdminProjectPage {
        items = List.copyOf(items);
    }
}
