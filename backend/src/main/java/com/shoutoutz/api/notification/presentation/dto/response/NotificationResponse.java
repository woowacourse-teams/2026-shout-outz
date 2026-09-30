package com.shoutoutz.api.notification.presentation.dto.response;

import com.shoutoutz.api.notification.domain.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        long notificationId,
        NotificationType notificationType,
        String message,
        Long feedId,
        String feedTitle,
        Long commentId,
        Actor actor,
        boolean isRead,
        Instant createdAt
) {

    public record Actor(
            Long userId,
            String handle,
            String displayName,
            String avatarUrl
    ) {
    }
}
