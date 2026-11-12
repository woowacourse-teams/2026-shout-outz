package com.shoutoutz.api.resourcereport.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.resourcereport.application.dto.ResourceReportAdminCursor;
import com.shoutoutz.api.resourcereport.domain.ResourceReportErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 관리자 자료 제보 목록 커서. 마지막 제보의 생성 시각과 id 를 이어 붙여 Base64URL 로 감싼다(버그 제보 커서와 같은 방식).
 */
@Component
public class ResourceReportAdminCursorCodec {

    private static final String DELIMITER = "|";

    String encode(ResourceReportAdminCursor cursor) {
        String value = cursor.createdAt() + DELIMITER + cursor.reportId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    ResourceReportAdminCursor decode(String encodedCursor) {
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
            long reportId = Long.parseLong(parts[1]);
            if (reportId < 1) {
                throw new IllegalArgumentException();
            }
            return new ResourceReportAdminCursor(Instant.parse(parts[0]), reportId);
        } catch (RuntimeException exception) {
            throw new BadRequestException(
                    ResourceReportErrorCode.RESOURCE_REPORT_ADMIN_CURSOR_INVALID,
                    exception
            );
        }
    }
}
