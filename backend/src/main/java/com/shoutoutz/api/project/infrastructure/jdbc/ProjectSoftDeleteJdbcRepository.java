package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.DeletedProject;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트의 소프트 삭제를 담당하는 JDBC 조작 클래스
 * 삭제 이력에 남길 slug와 title이 필요해서, 조회와 수정을 RETURNING 한 번으로 처리한다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectSoftDeleteJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 조건에 맞는 행이 없으면, 아무것도 수정하지 않고 빈 값을 돌려준다.
     * 삭제 요청이 동시에 들어와도 deleted_at IS NULL 조건 덕분에 한 번만 성공한다.
     */
    public Optional<DeletedProject> softDelete(long projectId, long registeredBy, Instant deletedAt) {
        return jdbcTemplate.query(
                        """
                                UPDATE projects
                                SET deleted_at = ?, updated_at = ?
                                WHERE id = ?
                                  AND registered_by = ?
                                  AND deleted_at IS NULL
                                RETURNING id, slug, title
                                """,
                        deletedProjectRowMapper(),
                        Timestamp.from(deletedAt),
                        Timestamp.from(deletedAt),
                        projectId,
                        registeredBy
                )
                .stream()
                .findFirst();
    }

    private RowMapper<DeletedProject> deletedProjectRowMapper() {
        return (resultSet, rowNumber) -> new DeletedProject(
                resultSet.getLong("id"),
                resultSet.getString("slug"),
                resultSet.getString("title")
        );
    }
}
