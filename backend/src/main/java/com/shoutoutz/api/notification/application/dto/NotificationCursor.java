package com.shoutoutz.api.notification.application.dto;

import java.time.Instant;

public record NotificationCursor(
        Instant createdAt,
        long notificationId
) {
}
