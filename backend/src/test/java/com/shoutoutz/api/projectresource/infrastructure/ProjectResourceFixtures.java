package com.shoutoutz.api.projectresource.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 프로젝트 기술 이야기 통합 테스트용 행을 넣는다. 테스트는 트랜잭션 안에서 돌아 끝나면 모두 지워진다.
 */
final class ProjectResourceFixtures {

    private final JdbcTemplate jdbcTemplate;

    ProjectResourceFixtures(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    long publicProject(String teamName) {
        return project(teamName, "APPROVED", false, null);
    }

    long project(String teamName, String approvalStatus, boolean deleted, Long thumbnailMediaId) {
        String slug = "resource-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject("""
                INSERT INTO projects (cohort, team_name, slug, title, tagline, github_repository_url, service_status,
                                      approval_status, thumbnail_media_id, deleted_at)
                VALUES (8, ?, ?, ?, '한 줄 소개', ?, 'OPERATING', ?, ?, CASE WHEN ? THEN now() END)
                RETURNING id
                """, Long.class, teamName, slug, teamName + " 프로젝트", "https://github.com/woowacourse-teams/" + slug,
                approvalStatus, thumbnailMediaId, deleted);
    }

    String slugOf(long projectId) {
        return jdbcTemplate.queryForObject("SELECT slug FROM projects WHERE id = ?", String.class, projectId);
    }

    long media() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO media_metadata (purpose, s3_key, mime_type, size_bytes, status, expires_at)
                VALUES ('USER_AVATAR', ?, 'image/webp', 1, 'READY', now())
                RETURNING id
                """, Long.class, "media/" + UUID.randomUUID());
    }

    long user() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "resource-" + UUID.randomUUID().toString().substring(0, 8)
        );
    }

    void removalReport(long projectId, String source, String externalKey, String status) {
        jdbcTemplate.update("""
                INSERT INTO resource_reports (project_id, user_id, kind, resource_source, resource_external_key, status,
                                              created_at, updated_at)
                VALUES (?, ?, 'REMOVE', ?, ?, ?, now(), now())
                """, projectId, user(), source, externalKey, status);
    }

    Document document(String source, String externalKey) {
        return new Document(source, externalKey);
    }

    final class Document {

        private final String source;
        private final String externalKey;
        private Long projectId;
        private String topic;
        private String track;
        private Integer score;
        private Instant publishedAt;
        private String extra = "{}";
        private boolean deleted;

        private Document(String source, String externalKey) {
            this.source = source;
            this.externalKey = externalKey;
        }

        Document project(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        Document topic(String topic) {
            this.topic = topic;
            return this;
        }

        Document track(String track) {
            this.track = track;
            return this;
        }

        Document score(Integer score) {
            this.score = score;
            return this;
        }

        Document publishedAt(String publishedAt) {
            this.publishedAt = Instant.parse(publishedAt);
            return this;
        }

        Document extra(String extra) {
            this.extra = extra;
            return this;
        }

        Document deleted() {
            this.deleted = true;
            return this;
        }

        long insert() {
            return jdbcTemplate.queryForObject("""
                    INSERT INTO resource_documents (source, external_key, version, title, url, summary, project_id,
                                                    track, published_at, score, prompt_version, extra, topic,
                                                    updated_at, deleted_at)
                    VALUES (?, ?, 1, ?, ?, ?, ?, ?, ?, ?, 'v1', CAST(? AS jsonb), ?, now(), CASE WHEN ? THEN now() END)
                    RETURNING id
                    """, Long.class, source, externalKey, "제목 " + externalKey, "https://example.com/" + externalKey,
                    "한 줄 " + externalKey, projectId, track, utc(publishedAt), score, extra, topic, deleted);
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
