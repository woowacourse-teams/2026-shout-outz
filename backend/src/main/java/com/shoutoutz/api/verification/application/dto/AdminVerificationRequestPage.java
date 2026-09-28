package com.shoutoutz.api.verification.application.dto;

import java.util.List;

public record AdminVerificationRequestPage(
        List<AdminVerificationRequestItem> items,
        boolean hasNext,
        long totalCount
) {

    public AdminVerificationRequestPage {
        items = List.copyOf(items);
    }
}
