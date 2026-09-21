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

    private static final String ACTIVE_ADMIN_EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM users
                WHERE id = ?
                  AND status = 'ACTIVE'
                  AND role = 'ADMIN'
            )
            """;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 홈 배너는 활성 관리자만, 나머지 용도는 활성 사용자만 업로드할 수 있다.
     */
    @Override
    public void authorize(long requesterId, MediaPurpose purpose) {
        if (requesterId <= 0 || purpose == null) {
            throw forbidden();
        }

        String authorizationSql = purpose == MediaPurpose.HOME_BANNER
                ? ACTIVE_ADMIN_EXISTS_SQL
                : ACTIVE_USER_EXISTS_SQL;
        if (!exists(authorizationSql, requesterId)) {
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
