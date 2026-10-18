package com.shoutoutz.api.magazine;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MagazineMigrationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void 기존_인터뷰_데이터와_미디어_경로를_보존하며_매거진으로_전환한다() throws Exception {
        String schema = "magazine_migration_" + UUID.randomUUID().toString().replace("-", "");
        try {
            migrate(schema, "20261016120000");
            try (Connection connection = dataSource.getConnection()) {
                String originalSchema = connection.getSchema();
                try {
                    connection.setSchema(schema);
                    JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                    ExistingData data = insertExistingData(jdbc);
                    migrate(schema, "latest");
                    assertMigratedData(jdbc, data);
                } finally {
                    connection.setSchema(originalSchema);
                }
            }
        } finally {
            new JdbcTemplate(dataSource).execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private ExistingData insertExistingData(JdbcTemplate jdbc) {
        long userId = jdbc.queryForObject(
                "INSERT INTO users (handle, role) VALUES ('@editor', 'ADMIN') RETURNING id", Long.class);
        long mediaId = jdbc.queryForObject("""
                INSERT INTO media_metadata (purpose, s3_key, mime_type, size_bytes, status, expires_at)
                VALUES ('INTERVIEW_CONTENT', 'media/interview-content/original.png',
                        'image/png', 1024, 'READY', now()) RETURNING id
                """, Long.class);
        String content = "![사진](media://" + mediaId + ")";
        long interviewId = jdbc.queryForObject("""
                INSERT INTO interviews (title, interviewee_id, author_id, content_md, comment_count, view_count,
                                        status, published_at)
                VALUES ('기존 글', ?, ?, ?, 1, 1, 'PUBLISHED', now()) RETURNING id
                """, Long.class, userId, userId, content);
        long commentId = jdbc.queryForObject("""
                INSERT INTO interview_comments (interview_id, author_id, content)
                VALUES (?, ?, '기존 댓글') RETURNING id
                """, Long.class, interviewId, userId);
        jdbc.update("INSERT INTO interview_reactions VALUES (?, ?, 'LIKE', now())", interviewId, userId);
        jdbc.update("INSERT INTO interview_comment_reactions VALUES (?, ?, 'AGREE', now())", commentId, userId);
        jdbc.update("""
                INSERT INTO interview_view_days (interview_id, visitor_key_hash, viewed_on)
                VALUES (?, 'visitor', current_date)
                """, interviewId);
        long cacheId = jdbc.queryForObject("""
                INSERT INTO link_preview_cache (url_hash, url)
                VALUES (repeat('a', 64), 'https://example.com') RETURNING id
                """, Long.class);
        jdbc.update("INSERT INTO interview_comment_link_preview_refs VALUES (?, ?)", commentId, cacheId);
        jdbc.update("""
                INSERT INTO notifications (recipient_id, actor_id, interview_id, interview_comment_id,
                                           notification_type, message)
                VALUES (?, ?, ?, ?, 'INTERVIEW_ACTIVITY', '기존 알림')
                """, userId, userId, interviewId, commentId);
        return new ExistingData(userId, mediaId, interviewId, commentId, cacheId, content);
    }

    private void assertMigratedData(JdbcTemplate jdbc, ExistingData data) {
        assertThat(jdbc.queryForMap("SELECT * FROM magazines WHERE id = ?", data.interviewId()))
                .containsEntry("featured_user_id", data.userId())
                .containsEntry("author_id", data.userId())
                .containsEntry("status", "PUBLISHED")
                .containsEntry("featured_user_introduction", null)
                .containsEntry("content_md", data.content())
                .containsEntry("comment_count", 1L)
                .containsEntry("view_count", 1L);
        assertThat(jdbc.queryForMap("SELECT * FROM media_metadata WHERE id = ?", data.mediaId()))
                .containsEntry("purpose", "MAGAZINE_CONTENT")
                .containsEntry("s3_key", "media/interview-content/original.png");
        assertThat(jdbc.queryForMap("SELECT * FROM magazine_comments WHERE id = ?", data.commentId()))
                .containsEntry("magazine_id", data.interviewId())
                .containsEntry("content", "기존 댓글");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM magazine_reactions", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM magazine_comment_reactions", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM magazine_view_days", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cache_id FROM magazine_comment_link_preview_refs", Long.class))
                .isEqualTo(data.cacheId());
        assertThat(jdbc.queryForMap("SELECT * FROM notifications"))
                .containsEntry("magazine_id", data.interviewId())
                .containsEntry("magazine_comment_id", data.commentId())
                .containsEntry("notification_type", "MAGAZINE_ACTIVITY");
        assertThat(jdbc.queryForObject("""
                INSERT INTO magazines (title, featured_user_id, author_id)
                VALUES ('새 글', ?, ?) RETURNING id
                """, Long.class, data.userId(), data.userId())).isGreaterThan(data.interviewId());
        assertThat(jdbc.queryForObject("""
                INSERT INTO magazine_comments (magazine_id, author_id, content)
                VALUES (?, ?, '새 댓글') RETURNING id
                """, Long.class, data.interviewId(), data.userId())).isGreaterThan(data.commentId());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = current_schema() AND c.relname LIKE '%interview%'
                """, Long.class)).isZero();
    }

    private record ExistingData(long userId, long mediaId, long interviewId, long commentId, long cacheId, String content) {
    }

    private void migrate(String schema, String target) {
        Flyway.configure()
                .configuration(Map.of("flyway.postgresql.transactional.lock", "false"))
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .target(target)
                .load()
                .migrate();
    }
}
