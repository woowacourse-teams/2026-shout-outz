package com.shoutoutz.api.notification.application.dto;

import java.util.List;

public record NotificationPage(
        List<NotificationItem> items,
        boolean hasNext,
        long totalCount
) {
}
