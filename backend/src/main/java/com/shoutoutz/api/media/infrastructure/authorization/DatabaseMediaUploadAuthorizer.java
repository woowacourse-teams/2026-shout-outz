package com.shoutoutz.api.media.infrastructure.authorization;

import com.shoutoutz.api.media.application.MediaUploadAuthorizer;
import com.shoutoutz.api.media.application.exception.MediaUploadForbiddenException;
import com.shoutoutz.api.media.domain.MediaPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 활성 사용자와 미디어 업로드 용도를 확인하는 미디어 업로드 권한 검증
 */
@Repository
@RequiredArgsConstructor
public class DatabaseMediaUploadAuthorizer implements MediaUploadAuthorizer {

    private static final String ACTIVE_USER_EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM users
                WHERE id = ?
                  AND status = 'ACTIVE'
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 실제 사용자가 활성 사용자 인지 검증한다.
     */
    @Override
    public void authorize(long requesterId, MediaPurpose purpose) {
        if (requesterId <= 0 || purpose == null) {
            throw forbidden();
        }

        if (!exists(ACTIVE_USER_EXISTS_SQL, requesterId)) {
            throw forbidden();
        }
    }

    private boolean exists(String sql, Object... arguments) {
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, arguments);
        return Boolean.TRUE.equals(result);
    }

    private static MediaUploadForbiddenException forbidden() {
        return new MediaUploadForbiddenException("미디어 업로드 권한이 없습니다.");
    }
}
