package com.shoutoutz.api.notification.application.dto;

import com.shoutoutz.api.notification.domain.NotificationType;
import java.time.Instant;

public record NotificationItem(
        long notificationId,
        NotificationType notificationType,
        String message,
        Long feedId,
        String feedTitle,
        Long commentId,
        Long actorId,
        String actorHandle,
        String actorDisplayName,
        Long actorAvatarImageId,
        boolean read,
        Instant createdAt
) {
}
