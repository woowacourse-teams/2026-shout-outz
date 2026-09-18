package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.ProjectViewRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 조회 기록(project_view_days)과 조회수(projects.view_count)를 함께 다룬다.
 * 조회수는 콘텐츠 수정이 아니므로 updated_at 은 바꾸지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class ProjectViewRepositoryImpl implements ProjectViewRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean existsViewableProject(long projectId) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1
                            FROM projects
                            WHERE id = :projectId
                              AND approval_status = 'APPROVED'
                              AND deleted_at IS NULL
                        )
                        """,
                new MapSqlParameterSource("projectId", projectId),
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 기록과 조회수 증가를 한 문장으로 처리한다.
     * 같은 방문자의 요청이 동시에 들어와도 기본 키 충돌로 한 건만 INSERT 되고, 조회수도 그 한 번만 오른다.
     */
    @Override
    public boolean record(long projectId, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt) {
        int updatedRows = jdbcTemplate.update(
                """
                        WITH recorded AS (
                            INSERT INTO project_view_days (project_id, visitor_key_hash, viewed_on, first_seen_at)
                            VALUES (:projectId, :visitorKeyHash, :viewedOn, :viewedAt)
                            ON CONFLICT (project_id, visitor_key_hash, viewed_on) DO NOTHING
                            RETURNING project_id
                        )
                        UPDATE projects
                        SET view_count = view_count + 1
                        WHERE id IN (SELECT project_id FROM recorded)
                        """,
                new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("visitorKeyHash", visitorKey.hash())
                        .addValue("viewedOn", viewedOn)
                        .addValue("viewedAt", Timestamp.from(viewedAt))
        );
        return updatedRows == 1;
    }
}
