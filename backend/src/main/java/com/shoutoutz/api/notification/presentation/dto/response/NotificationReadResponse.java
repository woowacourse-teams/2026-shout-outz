package com.shoutoutz.api.notification.presentation.dto.response;

public record NotificationReadResponse(
        long notificationId,
        boolean isRead
) {
}
