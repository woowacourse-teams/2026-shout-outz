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

    long project(String teamName, String approvalStatus, boolean deleted, Long logoMediaId) {
        String slug = "resource-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject("""
                INSERT INTO projects (cohort, team_name, slug, title, tagline, github_repository_url, service_status,
                                      approval_status, logo_media_id, deleted_at)
                VALUES (8, ?, ?, ?, '한 줄 소개', ?, 'OPERATING', ?, ?, CASE WHEN ? THEN now() END)
                RETURNING id
                """, Long.class, teamName + " 팀", slug, teamName, "https://github.com/woowacourse-teams/" + slug,
                approvalStatus, logoMediaId, deleted);
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

    /**
     * 경력 카드를 넣는다. 이력서 문장과 고려한 방법은 '|' 로 이어 받은 값을 배열로 넣는다.
     */
    long careerCard(long projectId, String track, String template, int sortOrder, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_career_cards (card_key, project_id, repo, track, template, title, category, problem,
                                                   cause, options, solution, result, resume_bullets, tech, written_by,
                                                   prompt_version, updated_at, sort_order, deleted_at)
                VALUES (?, ?, 'woowacourse-teams/repo', ?, ?, ?, '장애 대응', '문제', '원인',
                        string_to_array('방법 A|방법 B', '|'), '해결', '결과', string_to_array('문장 1|문장 2', '|'),
                        string_to_array('Spring|JPA', '|'), 'copy', 'v1', now(), ?, CASE WHEN ? THEN now() END)
                RETURNING id
                """, Long.class, "card-" + UUID.randomUUID().toString().substring(0, 12), projectId, track, template,
                "카드 " + track + " " + sortOrder, sortOrder, deleted);
    }

    long experience(long documentId, boolean deleted) {
        return experience(documentId, 0, deleted);
    }

    long experience(long documentId, int experienceIndex, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_experiences (document_id, experience_index, problem, solution, source, deleted_at)
                SELECT id, ?, '문제', '해결', source, CASE WHEN ? THEN now() END
                FROM resource_documents
                WHERE id = ?
                RETURNING id
                """, Long.class, experienceIndex, deleted, documentId);
    }

    void cardSource(long cardId, long experienceId, int sourceOrder) {
        jdbcTemplate.update(
                "INSERT INTO resource_career_card_sources (card_id, experience_id, source_order) VALUES (?, ?, ?)",
                cardId,
                experienceId,
                sourceOrder
        );
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
