package com.shoutoutz.api.verification.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.verification.application.dto.AdminVerificationRequestCursor;
import com.shoutoutz.api.verification.domain.UserVerificationErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 관리자 인증 신청 목록의 마지막 신청 시각과 ID를 opaque cursor로 변환한다.
 */
@Component
public class AdminVerificationRequestCursorCodec {

    private static final String DELIMITER = "|";

    String encode(AdminVerificationRequestCursor cursor) {
        String value = cursor.requestedAt() + DELIMITER + cursor.requestId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    AdminVerificationRequestCursor decode(String encodedCursor) {
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
            return new AdminVerificationRequestCursor(
                    Instant.parse(parts[0]),
                    Long.parseLong(parts[1])
            );
        } catch (RuntimeException exception) {
            throw new BadRequestException(
                    UserVerificationErrorCode.VERIFICATION_ADMIN_CURSOR_INVALID,
                    exception
            );
        }
    }
}
