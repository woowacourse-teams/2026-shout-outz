package com.shoutoutz.api.notification.domain;

public interface NotificationRepository {

    void createForFeedComment(long feedId, long commentId, long actorId);

    boolean markRead(long recipientId, long notificationId);

    void markAllRead(long recipientId);
}
