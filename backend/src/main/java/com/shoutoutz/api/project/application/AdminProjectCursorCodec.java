package com.shoutoutz.api.project.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.project.application.dto.AdminProjectCursor;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 관리자 프로젝트 목록의 등록 시각과 ID를 opaque cursor로 변환한다.
 */
@Component
public class AdminProjectCursorCodec {

    private static final String DELIMITER = "|";

    String encode(AdminProjectCursor cursor) {
        String value = cursor.createdAt() + DELIMITER + cursor.projectId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    AdminProjectCursor decode(String encodedCursor) {
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
            return new AdminProjectCursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new BadRequestException(ProjectErrorCode.PROJECT_APPROVAL_ADMIN_CURSOR_INVALID, exception);
        }
    }
}
