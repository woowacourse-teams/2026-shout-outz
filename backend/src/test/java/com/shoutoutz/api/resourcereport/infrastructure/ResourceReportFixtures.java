package com.shoutoutz.api.resourcereport.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 자료 제보 통합 테스트용 행을 넣는다. 테스트는 트랜잭션 안에서 돌아 끝나면 모두 지워진다.
 */
final class ResourceReportFixtures {

    private final JdbcTemplate jdbcTemplate;

    ResourceReportFixtures(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    long user() {
        String handle = "resource-report-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                handle
        );
    }

    long project(String title) {
        String slug = "report-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject("""
                INSERT INTO projects (cohort, team_name, slug, title, tagline, github_repository_url, service_status,
                                      approval_status)
                VALUES (8, '팀', ?, ?, '한 줄 소개', ?, 'OPERATING', 'APPROVED')
                RETURNING id
                """, Long.class, slug, title, "https://github.com/woowacourse-teams/" + slug);
    }

    String slugOf(long projectId) {
        return jdbcTemplate.queryForObject("SELECT slug FROM projects WHERE id = ?", String.class, projectId);
    }

    void member(long projectId, long userId) {
        jdbcTemplate.update("INSERT INTO project_members (project_id, user_id) VALUES (?, ?)", projectId, userId);
    }

    void archivedMember(long projectId, Long matchedUserId) {
        jdbcTemplate.update("""
                INSERT INTO woowa_archived_project_members (project_id, matched_user_id, github_account_id, github_login)
                VALUES (?, ?, '1234', 'archived-member')
                """, projectId, matchedUserId);
    }

    long document(String source, String externalKey, Long projectId, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_documents (source, external_key, version, title, url, prompt_version, updated_at,
                                                project_id, deleted_at)
                VALUES (?, ?, 1, ?, ?, 'v1', now(), ?, CASE WHEN ? THEN now() END)
                RETURNING id
                """, Long.class, source, externalKey, "제목 " + externalKey, "https://example.com/" + externalKey,
                projectId, deleted);
    }

    void deleteDocument(long documentId) {
        jdbcTemplate.update("UPDATE resource_documents SET deleted_at = now() WHERE id = ?", documentId);
    }

    long removalReport(long projectId, long userId, String source, String externalKey, String status, Instant createdAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_reports (project_id, user_id, kind, resource_source, resource_external_key, memo,
                                              status, created_at, updated_at)
                VALUES (?, ?, 'REMOVE', ?, ?, '필요 없어요', ?, ?, ?)
                RETURNING id
                """, Long.class, projectId, userId, source, externalKey, status, utc(createdAt), utc(createdAt));
    }

    long additionReport(long projectId, long userId, String url, String status, Instant createdAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_reports (project_id, user_id, kind, url, status, created_at, updated_at)
                VALUES (?, ?, 'ADD', ?, ?, ?, ?)
                RETURNING id
                """, Long.class, projectId, userId, url, status, utc(createdAt), utc(createdAt));
    }

    long countReports(long projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resource_reports WHERE project_id = ?",
                Long.class,
                projectId
        );
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
