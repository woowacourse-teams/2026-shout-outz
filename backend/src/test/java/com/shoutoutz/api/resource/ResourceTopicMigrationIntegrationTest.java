package com.shoutoutz.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResourceTopicMigrationIntegrationTest {

    private static final String SHOUT_OUTZ = "woowacourse-teams/2026-shout-outz";
    private static final String CRURU = "woowacourse-teams/2024-cruru";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 프로젝트_주제와_같은_고정_주제를_가진_다른_팀_자료를_찾는다() {
        long fileStorage = insertFixedTopic("file_storage");
        long crawling = insertProjectTopic(SHOUT_OUTZ, "크롤링/링크 미리보기", fileStorage);
        insertDocument(SHOUT_OUTZ, "링크 미리보기 수집 실패 해결", crawling);
        insertDocument(CRURU, "이미지 업로드 구조 개선", fileStorage);

        List<String> related = jdbc.queryForList("""
                SELECT d.title
                FROM resource_documents d
                JOIN resource_topics t ON t.id = d.topic_id
                WHERE COALESCE(t.anchor_topic_id, t.id) = (SELECT anchor_topic_id FROM resource_topics WHERE id = ?)
                  AND d.repo <> ?
                """, String.class, crawling, SHOUT_OUTZ);

        assertThat(related).containsExactly("이미지 업로드 구조 개선");
    }

    @Test
    void 프로젝트_주제는_가까운_고정_주제가_있어야_한다() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO resource_topics (topic_key, kind, name, side, project_key, updated_at)
                VALUES ('project:a:1', 'project', '크롤링/링크 미리보기', 'backend', ?, now())
                """, SHOUT_OUTZ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 고정_주제는_프로젝트에_속하지_않는다() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO resource_topics (topic_key, kind, name, side, project_key, updated_at)
                VALUES ('cache', 'fixed', '캐시(Redis 등)', 'backend', ?, now())
                """, SHOUT_OUTZ))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 주제가_지워지면_문서의_주제만_비운다() {
        long fileStorage = insertFixedTopic("file_storage");
        long crawling = insertProjectTopic(SHOUT_OUTZ, "크롤링/링크 미리보기", fileStorage);
        long documentId = insertDocument(SHOUT_OUTZ, "링크 미리보기 수집 실패 해결", crawling);

        jdbc.update("DELETE FROM resource_topics WHERE id = ?", crawling);

        assertThat(jdbc.queryForObject(
                "SELECT topic_id FROM resource_documents WHERE id = ?", Long.class, documentId)).isNull();
    }

    private long insertFixedTopic(String key) {
        return jdbc.queryForObject("""
                INSERT INTO resource_topics (topic_key, kind, name, side, category, updated_at)
                VALUES (?, 'fixed', '파일 업로드/이미지 처리', 'common', '협업, 운영', now())
                RETURNING id
                """, Long.class, key);
    }

    private long insertProjectTopic(String projectKey, String name, long anchorTopicId) {
        return jdbc.queryForObject("""
                INSERT INTO resource_topics (topic_key, kind, name, side, project_key, anchor_topic_id, updated_at)
                VALUES (?, 'project', ?, 'backend', ?, ?, now())
                RETURNING id
                """, Long.class, "project:" + projectKey + ":" + name.hashCode(), name, projectKey, anchorTopicId);
    }

    private long insertDocument(String repo, String title, long topicId) {
        return jdbc.queryForObject("""
                INSERT INTO resource_documents (source, external_key, version, title, url, repo, topic_id,
                                                prompt_version, updated_at)
                VALUES ('github-pr', gen_random_uuid()::text, 1, ?, 'https://github.com', ?, ?, 'llm-v7', now())
                RETURNING id
                """, Long.class, title, repo, topicId);
    }
}
