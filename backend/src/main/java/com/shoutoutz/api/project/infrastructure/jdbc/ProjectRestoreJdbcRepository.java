package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.RestorableProject;
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
                                SELECT d.id, d.restore_deadline_at, p.approval_status
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

    private RowMapper<RestorableProject> restorableProjectRowMapper() {
        return (resultSet, rowNumber) -> new RestorableProject(
                resultSet.getLong("id"),
                resultSet.getTimestamp("restore_deadline_at").toInstant(),
                ApprovalStatus.valueOf(resultSet.getString("approval_status"))
        );
    }
}
