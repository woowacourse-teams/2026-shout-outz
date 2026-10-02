package com.shoutoutz.api.project.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 프로젝트의 projects 행과 기술 태그를 직접 수정하는 관리자 저장소. */
@Repository
@RequiredArgsConstructor
public class AdminProjectMigrationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * columnValues의 키는 서비스의 고정된 컬럼 목록에서만 온다. 값은 모두 바인딩한다.
     * 삭제되지 않은 활성 프로젝트를 등록자 여부와 관계없이 대상으로 한다.
     */
    public Optional<Instant> update(long projectId, Map<String, Object> columnValues) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("projectId", projectId);
        String assignments = columnValues.entrySet().stream()
                .map(entry -> {
                    parameters.addValue(entry.getKey(), entry.getValue());
                    return entry.getKey() + " = :" + entry.getKey();
                })
                .collect(Collectors.joining(", "));
        if (!columnValues.containsKey("updated_at")) {
            assignments += assignments.isEmpty() ? "updated_at = now()" : ", updated_at = now()";
        }
        String sql = "UPDATE projects SET " + assignments + " "
                + "WHERE id = :projectId AND deleted_at IS NULL "
                + "RETURNING updated_at";
        return jdbcTemplate.query(sql, parameters, resultSet -> {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(resultSet.getObject("updated_at", OffsetDateTime.class).toInstant());
        });
    }

    /** 전달된 순서대로 기술 태그 목록 전체를 교체한다. 팀원 테이블은 건드리지 않는다. */
    public void replaceTechTags(long projectId, List<Long> techTagIds) {
        jdbcTemplate.update(
                "DELETE FROM project_tags WHERE project_id = :projectId",
                new MapSqlParameterSource("projectId", projectId)
        );
        for (int order = 0; order < techTagIds.size(); order++) {
            jdbcTemplate.update(
                    """
                            INSERT INTO project_tags (project_id, tech_tag_id, display_order)
                            VALUES (:projectId, :techTagId, :displayOrder)
                            """,
                    new MapSqlParameterSource()
                            .addValue("projectId", projectId)
                            .addValue("techTagId", techTagIds.get(order))
                            .addValue("displayOrder", order)
            );
        }
    }
}
