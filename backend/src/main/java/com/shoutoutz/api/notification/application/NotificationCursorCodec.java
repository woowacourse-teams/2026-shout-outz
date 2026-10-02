package com.shoutoutz.api.notification.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.notification.application.dto.NotificationCursor;
import com.shoutoutz.api.notification.domain.NotificationErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class NotificationCursorCodec {

    private static final String DELIMITER = "|";

    String encode(NotificationCursor cursor) {
        String value = cursor.createdAt() + DELIMITER + cursor.notificationId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    NotificationCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }

            Instant createdAt = Instant.parse(parts[0]);
            long notificationId = Long.parseLong(parts[1]);
            if (notificationId <= 0) {
                throw new IllegalArgumentException();
            }
            return new NotificationCursor(createdAt, notificationId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BadRequestException(
                    NotificationErrorCode.INVALID_NOTIFICATION_CURSOR,
                    exception
            );
        }
    }
}
