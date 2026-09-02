package com.shoutoutz.api.media.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MediaSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 미디어_마이그레이션은_기존_URL_컬럼을_제거하고_미디어_ID_참조를_생성한다() {
        assertThat(columnCount("projects", "thumbnail_url")).isZero();
        assertThat(columnCount("user_profiles", "avatar_url")).isZero();
        assertThat(columnCount("projects", "thumbnail_media_id")).isEqualTo(1);
        assertThat(columnCount("user_profiles", "avatar_image_id")).isEqualTo(1);

        assertThat(setNullForeignKeyCount("projects", "projects_thumbnail_media_fk")).isEqualTo(1);
        assertThat(setNullForeignKeyCount("user_profiles", "user_profiles_avatar_image_fk")).isEqualTo(1);
    }

    private int columnCount(String tableName, String columnName) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
    }

    private int setNullForeignKeyCount(String tableName, String constraintName) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        WHERE t.relname = ?
                          AND c.conname = ?
                          AND c.contype = 'f'
                          AND c.confdeltype = 'n'
                        """,
                Integer.class,
                tableName,
                constraintName
        );
    }
}
