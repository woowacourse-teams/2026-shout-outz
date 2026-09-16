package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.RestorableProject;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 복구를 담당하는 JDBC 조작 클래스
 * 복구 가능 여부를 한 번에 판단하도록 삭제된 프로젝트와 미복구 삭제 이력을 조인해 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectRestoreJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 없는 프로젝트, 남의 프로젝트, 삭제되지 않은 프로젝트, 이미 복구된 이력은 모두 빈 값이다.
     * 시스템이 영구 삭제한 이력은 복구 대상이 아니므로 제외한다.
     */
    public Optional<RestorableProject> findRestorable(long projectId, long registeredBy) {
        return jdbcTemplate.query(
                        """
                                SELECT d.id, d.restore_deadline_at
                                FROM project_deletions d
                                JOIN projects p ON p.id = d.project_id
                                WHERE d.project_id = ?
                                  AND p.registered_by = ?
                                  AND p.deleted_at IS NOT NULL
                                  AND d.restored_at IS NULL
                                  AND d.deletion_type <> 'SYSTEM_PURGE'
                                """,
                        restorableProjectRowMapper(),
                        projectId,
                        registeredBy
                )
                .stream()
                .findFirst();
    }

    /**
     * 조건에 맞는 행이 없으면 아무것도 수정하지 않고 빈 값을 돌려준다.
     * 복구 요청이 동시에 들어와도 deleted_at IS NOT NULL 조건 덕분에 한 번만 성공한다.
     * 복구 응답에 쓰는 승인 상태는 복구 시점 값을 함께 받아 온다.
     */
    public Optional<ApprovalStatus> restoreProject(long projectId, Instant restoredAt) {
        return jdbcTemplate.query(
                        """
                                UPDATE projects
                                SET deleted_at = NULL, updated_at = ?
                                WHERE id = ?
                                  AND deleted_at IS NOT NULL
                                RETURNING approval_status
                                """,
                        (resultSet, rowNumber) -> ApprovalStatus.valueOf(resultSet.getString("approval_status")),
                        Timestamp.from(restoredAt),
                        projectId
                )
                .stream()
                .findFirst();
    }

    /**
     * 복구할 때는 새 이력을 만들지 않고 미복구 이력에 복구 주체와 복구 시각을 채운다.
     */
    public int restoreDeletion(long deletionId, long restoredBy, Instant restoredAt) {
        return jdbcTemplate.update(
                """
                        UPDATE project_deletions
                        SET restored_by = ?, restored_at = ?
                        WHERE id = ?
                          AND restored_at IS NULL
                        """,
                restoredBy,
                Timestamp.from(restoredAt),
                deletionId
        );
    }

    private RowMapper<RestorableProject> restorableProjectRowMapper() {
        return (resultSet, rowNumber) -> new RestorableProject(
                resultSet.getLong("id"),
                resultSet.getTimestamp("restore_deadline_at").toInstant()
        );
    }
}
