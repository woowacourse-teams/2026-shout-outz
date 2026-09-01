package com.shoutoutz.api.media.infrastructure.authorization;

import com.shoutoutz.api.media.application.MediaUploadAuthorizer;
import com.shoutoutz.api.media.application.MediaUploadForbiddenException;
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

    private static final String POST_AUTHOR_EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM users u
                JOIN posts p ON p.author_id = u.id
                WHERE u.id = ?
                  AND u.status = 'ACTIVE'
                  AND p.id = ?
                  AND p.deleted_at IS NULL
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void authorize(long requesterId, MediaPurpose purpose, long targetId) {
        if (requesterId <= 0 || targetId <= 0 || purpose == null) {
            throw forbidden();
        }

        boolean authorized = switch (purpose) {
            case USER_AVATAR -> requesterId == targetId && exists(
                    ACTIVE_USER_EXISTS_SQL,
                    requesterId
            );
            case PROJECT_THUMBNAIL, PROJECT_DESCRIPTION -> exists(
                    PROJECT_EDITOR_EXISTS_SQL,
                    targetId,
                    requesterId
            );
            case POST_CONTENT -> exists(
                    POST_AUTHOR_EXISTS_SQL,
                    requesterId,
                    targetId
            );
        };

        if (!authorized) {
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
