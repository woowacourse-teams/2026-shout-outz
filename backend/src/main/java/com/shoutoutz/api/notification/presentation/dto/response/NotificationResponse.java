package com.shoutoutz.api.notification.presentation.dto.response;

import com.shoutoutz.api.notification.domain.NotificationType;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;

public record NotificationResponse(
        long notificationId,
        NotificationType notificationType,
        String message,
        Long feedId,
        String feedTitle,
        Long commentId,
        Actor actor,
        boolean isRead,
        Instant createdAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long interviewId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String interviewTitle,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long interviewCommentId
) {

    public NotificationResponse(long notificationId, NotificationType notificationType, String message,
            Long feedId, String feedTitle, Long commentId, Actor actor, boolean isRead, Instant createdAt) {
        this(notificationId, notificationType, message, feedId, feedTitle, commentId, actor, isRead,
                createdAt, null, null, null);
    }

    public record Actor(
            Long userId,
            String handle,
            String displayName,
            String avatarUrl
    ) {
    }
}
