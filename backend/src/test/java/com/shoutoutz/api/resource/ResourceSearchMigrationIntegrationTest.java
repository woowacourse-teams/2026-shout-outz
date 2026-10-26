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
class ResourceSearchMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void 문서와_경험과_소개_경험_조각을_저장하고_조각에서_경험_카드로_이어진다() {
        long documentId = insertDocument("tech_decision");
        long experienceId = insertExperience(documentId, 0);
        insertChunk(documentId, 0, "intro", null);
        insertChunk(documentId, 1, "experience", experienceId);

        List<String> titles = jdbc.queryForList("""
                SELECT e.title
                FROM resource_chunks c
                JOIN resource_experiences e ON e.id = c.experience_id
                WHERE c.document_id = ?
                """, String.class, documentId);
        List<String> keyPoints = jdbc.queryForList(
                "SELECT unnest(key_points) FROM resource_documents WHERE id = ?", String.class, documentId);

        assertThat(titles).containsExactly("링크 미리보기 수집 실패 해결");
        assertThat(keyPoints).containsExactly("운영 응답을 curl로 비교하는 방법");
    }

    @Test
    void 문서를_지우면_경험과_조각도_함께_지워진다() {
        long documentId = insertDocument("problem_solving");
        long experienceId = insertExperience(documentId, 0);
        insertChunk(documentId, 1, "experience", experienceId);

        jdbc.update("DELETE FROM resource_documents WHERE id = ?", documentId);

        assertThat(count("resource_experiences", documentId)).isZero();
        assertThat(count("resource_chunks", documentId)).isZero();
    }

    @Test
    void 정해진_종류가_아니면_문서를_저장하지_않는다() {
        assertThatThrownBy(() -> insertDocument("unknown"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 경험_조각에_경험이_없으면_저장하지_않는다() {
        long documentId = insertDocument("chore");

        assertThatThrownBy(() -> insertChunk(documentId, 1, "experience", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 소개_조각에_경험이_있으면_저장하지_않는다() {
        long documentId = insertDocument("chore");
        long experienceId = insertExperience(documentId, 0);

        assertThatThrownBy(() -> insertChunk(documentId, 0, "intro", experienceId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertDocument(String contentType) {
        return jdbc.queryForObject("""
                INSERT INTO resource_documents (source, external_key, version, title, url, summary, track,
                                                score, content_type, score_reason, key_points,
                                                prompt_version, updated_at)
                VALUES ('github-pr', gen_random_uuid()::text, 1, '링크 미리보기 수집 실패 해결',
                        'https://github.com/woowacourse-teams/2026-shout-outz/pull/388', '한 줄 안내', 'BE',
                        85, ?, '원인과 선택 이유가 뚜렷해요', ARRAY['운영 응답을 curl로 비교하는 방법'],
                        'llm-v7', now())
                RETURNING id
                """, Long.class, contentType);
    }

    private long insertExperience(long documentId, int experienceIndex) {
        return jdbc.queryForObject("""
                INSERT INTO resource_experiences (document_id, experience_index, title, category, problem,
                                                  solution, tech, resume_bullet, source, track)
                VALUES (?, ?, '링크 미리보기 수집 실패 해결', '장애 대응', '운영에서 미리보기가 비었습니다.',
                        'oEmbed API로 바꿨습니다.', ARRAY['YouTube oEmbed'], '미리보기 실패를 없앴습니다.',
                        'github-pr', 'BE')
                RETURNING id
                """, Long.class, documentId, experienceIndex);
    }

    private void insertChunk(long documentId, int chunkIndex, String chunkKind, Long experienceId) {
        jdbc.update("""
                INSERT INTO resource_chunks (document_id, chunk_index, chunk_kind, chunk_text, token_count,
                                             embedding, embedding_model, source, experience_id)
                VALUES (?, ?, ?, '조각 글', 10, array_fill(0.03125, ARRAY[1024])::vector,
                        'text-embedding-3-small@1024', 'github-pr', ?)
                """, documentId, chunkIndex, chunkKind, experienceId);
    }

    private int count(String table, long documentId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE document_id = ?", Integer.class, documentId);
    }
}
