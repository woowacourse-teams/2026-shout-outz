package com.shoutoutz.api.notification.application;

import com.shoutoutz.api.notification.application.dto.NotificationCursor;
import com.shoutoutz.api.notification.application.dto.NotificationPage;

public interface NotificationQueryRepository {

    NotificationPage findAll(long recipientId, NotificationCursor cursor, int size);

    long countUnread(long recipientId);
}
