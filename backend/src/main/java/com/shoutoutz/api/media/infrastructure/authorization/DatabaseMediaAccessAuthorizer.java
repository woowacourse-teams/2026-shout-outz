package com.shoutoutz.api.media.infrastructure.authorization;

import com.shoutoutz.api.media.application.MediaAccessAuthorizer;
import com.shoutoutz.api.media.application.exception.MediaAccessForbiddenException;
import com.shoutoutz.api.media.application.exception.MediaAccessUnauthorizedException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 현재 스키마의 게시글 연결과 업로더 소유권을 이용한 미디어 조회 권한 확인.
 *
 * <p>삭제되지 않은 게시글에 명시적으로 연결된 본문 미디어는 게시글 렌더링을 위해
 * 공개 조회를 허용한다. 그 외 미디어는 업로더 본인만 조회할 수 있다.</p>
 */
@Repository
@RequiredArgsConstructor
public class DatabaseMediaAccessAuthorizer implements MediaAccessAuthorizer {

    private static final String PUBLIC_POST_MEDIA_EXISTS_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM post_media pm
                JOIN posts p ON p.id = pm.post_id
                JOIN media_metadata m ON m.id = pm.media_metadata_id
                WHERE pm.media_metadata_id = ?
                  AND m.purpose = 'POST_CONTENT'
                  AND p.deleted_at IS NULL
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
        if (isPublicPostMedia(metadata.getId())) {
            return;
        }
        if (requesterId == null) {
            throw unauthorized();
        }
        if (!requesterId.equals(metadata.getUploadedBy())) {
            throw forbidden();
        }
    }

    private boolean isPublicPostMedia(long mediaId) {
        Boolean result = jdbcTemplate.queryForObject(
                PUBLIC_POST_MEDIA_EXISTS_SQL,
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
