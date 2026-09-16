package com.shoutoutz.api.media.infrastructure.authorization;

import com.shoutoutz.api.media.application.MediaUploadAuthorizer;
import com.shoutoutz.api.media.application.exception.MediaUploadForbiddenException;
import com.shoutoutz.api.media.domain.MediaPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 현재 스키마의 소유자, 팀원, 작성자 관계를 이용한 미디어 업로드 권한 확인
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

    private static final String PROJECT_EDITOR_EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM users u
                JOIN projects p ON p.id = ?
                WHERE u.id = ?
                  AND u.status = 'ACTIVE'
                  AND p.deleted_at IS NULL
                  AND (
                      p.registered_by = u.id
                      OR EXISTS (
                          SELECT 1
                          FROM project_members pm
                          WHERE pm.project_id = p.id
                            AND pm.user_id = u.id
                      )
                  )
            )
            """;

    private static final String FEED_AUTHOR_EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM users u
                JOIN feeds p ON p.author_id = u.id
                WHERE u.id = ?
                  AND u.status = 'ACTIVE'
                  AND p.id = ?
                  AND p.deleted_at IS NULL
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void authorize(long requesterId, MediaPurpose purpose, Long targetId) {
        if (requesterId <= 0 || purpose == null) {
            throw forbidden();
        }

        boolean authorized = switch (purpose) {
            case USER_AVATAR -> hasTarget(targetId) && requesterId == targetId && exists(
                    ACTIVE_USER_EXISTS_SQL,
                    requesterId
            );
            case PROJECT_THUMBNAIL, PROJECT_DESCRIPTION -> hasTarget(targetId) && exists(
                    PROJECT_EDITOR_EXISTS_SQL,
                    targetId,
                    requesterId
            );
            case FEED_CONTENT -> hasTarget(targetId) && exists(
                    FEED_AUTHOR_EXISTS_SQL,
                    requesterId,
                    targetId
            );
            case HOME_BANNER -> targetId == null && exists(
                    ACTIVE_ADMIN_EXISTS_SQL,
                    requesterId
            );
        };

        if (!authorized) {
            throw forbidden();
        }
    }

    private boolean hasTarget(Long targetId) {
        return targetId != null && targetId > 0;
    }

    private boolean exists(String sql, Object... arguments) {
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, arguments);
        return Boolean.TRUE.equals(result);
    }

    private static MediaUploadForbiddenException forbidden() {
        return new MediaUploadForbiddenException("미디어 업로드 권한이 없습니다.");
    }
}
