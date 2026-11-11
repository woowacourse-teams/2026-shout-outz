package com.shoutoutz.api.resourcesearch.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class ResourceDocumentFixture {

    private final JdbcTemplate jdbcTemplate;

    public ResourceDocumentFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DocumentBuilder document(String source) {
        return new DocumentBuilder(source);
    }

    public long approvedProject(String slug, String teamName) {
        return project(slug, teamName, "APPROVED", null);
    }

    public long approvedProjectWithLogo(String slug, String teamName) {
        long userId = user();
        long mediaId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO media_metadata (
                            uploaded_by, purpose, s3_key, mime_type,
                            size_bytes, status, expires_at, uploaded_at
                        ) VALUES (?, 'PROJECT_LOGO', ?, 'image/webp', 1024, 'READY', now(), now())
                        RETURNING id
                        """,
                Long.class,
                userId, "media/project/" + UUID.randomUUID()
        );
        long projectId = project(slug, teamName, "APPROVED", null);
        jdbcTemplate.update("UPDATE projects SET logo_media_id = ? WHERE id = ?", mediaId, projectId);
        return projectId;
    }

    public long pendingProject(String slug, String teamName) {
        return project(slug, teamName, "PENDING", null);
    }

    public long deletedProject(String slug, String teamName) {
        return project(slug, teamName, "APPROVED", Timestamp.from(Instant.now()));
    }

    public void removalReport(long documentId, String status) {
        long projectId = approvedProject("report-" + UUID.randomUUID().toString().substring(0, 8), "제보 팀");
        jdbcTemplate.update(
                """
                        INSERT INTO resource_reports (
                            project_id, user_id, kind, resource_source, resource_external_key, status, created_at, updated_at
                        )
                        SELECT ?, ?, 'REMOVE', source, external_key, ?, now(), now()
                        FROM resource_documents
                        WHERE id = ?
                        """,
                projectId, user(), status, documentId
        );
    }

    public long logoMediaIdOf(long projectId) {
        return jdbcTemplate.queryForObject("SELECT logo_media_id FROM projects WHERE id = ?", Long.class, projectId);
    }

    private long project(String slug, String teamName, String approvalStatus, Timestamp deletedAt) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, team_name, slug, title, tagline,
                            service_status, approval_status, github_repository_url, deleted_at
                        ) VALUES (8, ?, ?, '테스트 프로젝트', '프로젝트 소개', 'OPERATING', ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                teamName, slug, approvalStatus, "https://github.com/woowacourse-teams/" + slug, deletedAt
        );
    }

    private long user() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle, role) VALUES (?, 'USER') RETURNING id",
                Long.class,
                "@user-" + UUID.randomUUID().toString().substring(0, 12)
        );
    }

    public final class DocumentBuilder {

        private final String source;
        private String title = "자료 제목";
        private String summary = "한줄 소개";
        private Instant publishedAt = Instant.parse("2026-10-20T01:00:00Z");
        private Long projectId;
        private String extra = "{}";
        private String contentType;
        private String topic;
        private String[] keyPoints = {};
        private String[] techTags = {};
        private boolean deleted;

        private DocumentBuilder(String source) {
            this.source = source;
        }

        public DocumentBuilder title(String title) {
            this.title = title;
            return this;
        }

        public DocumentBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public DocumentBuilder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public DocumentBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public DocumentBuilder extra(String extra) {
            this.extra = extra;
            return this;
        }

        public DocumentBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public DocumentBuilder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public DocumentBuilder keyPoints(String... keyPoints) {
            this.keyPoints = keyPoints;
            return this;
        }

        public DocumentBuilder techTags(String... techTags) {
            this.techTags = techTags;
            return this;
        }

        public DocumentBuilder deleted() {
            this.deleted = true;
            return this;
        }

        public long save() {
            String externalKey = UUID.randomUUID().toString();
            return jdbcTemplate.queryForObject(
                    """
                            INSERT INTO resource_documents (
                                source, external_key, version, title, url, summary, project_id, published_at,
                                content_type, topic, key_points, tech_tags, prompt_version, extra, updated_at, deleted_at
                            ) VALUES (?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'test-v1', CAST(? AS jsonb), now(), ?)
                            RETURNING id
                            """,
                    Long.class,
                    source, externalKey, title, "https://example.com/" + externalKey, summary, projectId,
                    publishedAt == null ? null : Timestamp.from(publishedAt),
                    contentType, topic, keyPoints, techTags, extra,
                    deleted ? Timestamp.from(Instant.now()) : null
            );
        }
    }
}
