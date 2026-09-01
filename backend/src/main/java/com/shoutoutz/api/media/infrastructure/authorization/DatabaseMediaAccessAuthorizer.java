package com.shoutoutz.api.media.infrastructure.authorization;

import com.shoutoutz.api.media.application.MediaAccessAuthorizer;
import com.shoutoutz.api.media.application.exception.MediaAccessForbiddenException;
import com.shoutoutz.api.media.application.exception.MediaAccessUnauthorizedException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 미디어 조회 권한 확인.
 *
 * <p>삭제되지 않은 게시글에 연결된 본문 미디어, 승인되고 삭제되지 않은 프로젝트의
 * 썸네일, 삭제되지 않은 사용자의 프로필 이미지는 공개 조회를 허용한다. 그 외 미디어는
 * 업로더 본인만 조회할 수 있다.</p>
 */
@Repository
@RequiredArgsConstructor
public class DatabaseMediaAccessAuthorizer implements MediaAccessAuthorizer {

    private static final String PUBLIC_MEDIA_EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM media_metadata m
                WHERE m.id = ?
                  AND (
                      (
                          m.purpose = 'POST_CONTENT'
                          AND EXISTS (
                              SELECT 1
                              FROM post_media pm
                              JOIN posts p ON p.id = pm.post_id
                              WHERE pm.media_metadata_id = m.id
                                AND p.deleted_at IS NULL
                          )
                      )
                      OR (
                          m.purpose = 'PROJECT_THUMBNAIL'
                          AND EXISTS (
                              SELECT 1
                              FROM projects p
                              WHERE p.thumbnail_media_id = m.id
                                AND p.approval_status = 'APPROVED'
                                AND p.deleted_at IS NULL
                          )
                      )
                      OR (
                          m.purpose = 'USER_AVATAR'
                          AND EXISTS (
                              SELECT 1
                              FROM user_profiles up
                              JOIN users u ON u.id = up.user_id
                              WHERE up.avatar_media_id = m.id
                                AND u.deleted_at IS NULL
                          )
                      )
                  )
            )
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void authorize(Long requesterId, MediaMetadata metadata) {
        if (metadata == null || metadata.getId() == null) {
            throw forbidden();
        }
        if (requesterId != null && requesterId <= 0) {
            throw unauthorized();
        }
        if (isPublicMedia(metadata.getId())) {
            return;
        }
        if (requesterId == null) {
            throw unauthorized();
        }
        if (!requesterId.equals(metadata.getUploadedBy())) {
            throw forbidden();
        }
    }

    private boolean isPublicMedia(long mediaId) {
        Boolean result = jdbcTemplate.queryForObject(
                PUBLIC_MEDIA_EXISTS_SQL,
                Boolean.class,
                mediaId
        );
        return Boolean.TRUE.equals(result);
    }

    private static MediaAccessForbiddenException forbidden() {
        return new MediaAccessForbiddenException("미디어 조회 권한이 없습니다.");
    }

    private static MediaAccessUnauthorizedException unauthorized() {
        return new MediaAccessUnauthorizedException("미디어 조회를 위해 인증이 필요합니다.");
    }
}
