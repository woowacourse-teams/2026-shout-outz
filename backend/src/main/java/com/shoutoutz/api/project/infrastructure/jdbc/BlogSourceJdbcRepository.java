package com.shoutoutz.api.project.infrastructure.jdbc;

import com.shoutoutz.api.project.domain.BlogSourceRepository;
import com.shoutoutz.api.project.domain.TeamBlogUrls;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 팀 블로그 원천 등록부를 다루는 JDBC 저장소
 * 지우지 않고 상태를 바꾸는 갱신과 충돌 시 되살리는 삽입이 필요해, JPA 대신 SQL 로 처리한다.
 */
@Repository
@RequiredArgsConstructor
public class BlogSourceJdbcRepository implements BlogSourceRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void syncServiceInputs(long projectId, TeamBlogUrls teamBlogUrls) {
        List<String> siteUrls = teamBlogUrls.values();
        disableRemoved(projectId, siteUrls);
        confirm(projectId, siteUrls);
    }

    /**
     * 프로젝트 상세에 보여 줄 확정된 블로그 주소를 노출 순서대로 조회한다. 팀 입력과 운영자 입력을 함께 보여 준다.
     */
    public List<String> findConfirmedSiteUrls(long projectId) {
        String sql = """
                SELECT site_url
                FROM blog_sources
                WHERE project_id = :projectId
                  AND status = 'CONFIRMED'
                ORDER BY display_order, id
                """;
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("projectId", projectId), String.class);
    }

    private void disableRemoved(long projectId, List<String> siteUrls) {
        String sql = """
                UPDATE blog_sources
                SET status = 'DISABLED', updated_at = now()
                WHERE project_id = :projectId
                  AND kind = 'SERVICE_INPUT'
                  AND status = 'CONFIRMED'
                """ + (siteUrls.isEmpty() ? "" : "  AND site_url NOT IN (:siteUrls)\n");
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("siteUrls", siteUrls));
    }

    /**
     * 이미 있는 주소는 다시 CONFIRMED 로 두고 노출 순서만 바꾼다. 팀이 지웠다가 다시 넣은 주소도 이렇게 되살아난다.
     * 운영자가 넣은 같은 주소(MANUAL)는 운영자 판단을 따르도록 바꾸지 않는다.
     */
    private void confirm(long projectId, List<String> siteUrls) {
        if (siteUrls.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO blog_sources (project_id, site_url, kind, status, display_order)
                VALUES (:projectId, :siteUrl, 'SERVICE_INPUT', 'CONFIRMED', :displayOrder)
                ON CONFLICT (project_id, site_url) DO UPDATE
                SET status = 'CONFIRMED',
                    display_order = EXCLUDED.display_order,
                    updated_at = now()
                WHERE blog_sources.kind = 'SERVICE_INPUT'
                """;
        MapSqlParameterSource[] parameters = IntStream.range(0, siteUrls.size())
                .mapToObj(order -> new MapSqlParameterSource()
                        .addValue("projectId", projectId)
                        .addValue("siteUrl", siteUrls.get(order))
                        .addValue("displayOrder", order))
                .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(sql, parameters);
    }
}
