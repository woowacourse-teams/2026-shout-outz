package com.shoutoutz.api.bugreport.application;

import com.shoutoutz.api.bugreport.application.dto.BugReportCursor;
import com.shoutoutz.api.bugreport.domain.BugReportErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class BugReportCursorCodec {

    private static final String DELIMITER = "|";

    String encode(BugReportCursor cursor) {
        String value = cursor.createdAt() + DELIMITER + cursor.bugReportId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    BugReportCursor decode(String encodedCursor) {
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
            long bugReportId = Long.parseLong(parts[1]);
            if (bugReportId < 1) {
                throw new IllegalArgumentException();
            }
            return new BugReportCursor(Instant.parse(parts[0]), bugReportId);
        } catch (RuntimeException exception) {
            throw new BadRequestException(
                    BugReportErrorCode.BUG_REPORT_ADMIN_CURSOR_INVALID,
                    exception
            );
        }
    }
}
