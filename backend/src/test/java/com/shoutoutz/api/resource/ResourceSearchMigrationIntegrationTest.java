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

    @Test
    void 같은_기술_주제를_가진_다른_팀_자료를_찾는다() {
        long mine = insertDocument("problem_solving");
        long other = insertDocument("problem_solving");
        jdbc.update("UPDATE resource_documents SET topic = 'cache', repo = 'woowacourse-teams/2026-shout-outz' WHERE id = ?", mine);
        jdbc.update("UPDATE resource_documents SET topic = 'cache', repo = 'woowacourse-teams/2024-cruru' WHERE id = ?", other);

        List<Long> related = jdbc.queryForList("""
                SELECT id FROM resource_documents
                WHERE topic = 'cache' AND repo <> 'woowacourse-teams/2026-shout-outz' AND deleted_at IS NULL
                """, Long.class);

        assertThat(related).containsExactly(other);
    }

    @Test
    void 기술_주제_목록에_77개_주제와_직군이_들어_있다() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM resource_topic_catalog", Integer.class);
        String cacheSide = jdbc.queryForObject("SELECT side FROM resource_topic_catalog WHERE id = 'cache'", String.class);

        assertThat(count).isEqualTo(77);
        assertThat(cacheSide).isEqualTo("backend");
    }

    @Test
    void 목록에_없는_기술_주제는_문서에_저장하지_않는다() {
        long documentId = insertDocument("chore");

        assertThatThrownBy(() -> jdbc.update("UPDATE resource_documents SET topic = 'unknown' WHERE id = ?", documentId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 기술_주제와_고른_근거를_함께_저장한다() {
        long documentId = insertDocument("problem_solving");
        jdbc.update("UPDATE resource_documents SET topic = 'cache', topic_reason = ? WHERE id = ?",
                "조회 결과를 Redis 에 캐시한 과정이 중심이에요.", documentId);

        String reason = jdbc.queryForObject(
                "SELECT topic_reason FROM resource_documents WHERE id = ?", String.class, documentId);

        assertThat(reason).isEqualTo("조회 결과를 Redis 에 캐시한 과정이 중심이에요.");
    }

    @Test
    void 이력서_항목을_정해진_순서대로_읽는다() {
        long second = insertCard("card:a");
        long first = insertCard("card:b");
        jdbc.update("UPDATE resource_career_cards SET sort_order = 1 WHERE id = ?", second);
        jdbc.update("UPDATE resource_career_cards SET sort_order = 0 WHERE id = ?", first);

        List<Long> ordered = jdbc.queryForList("""
                SELECT id FROM resource_career_cards
                WHERE repo = 'woowacourse-teams/2026-shout-outz' AND track = 'BE' AND deleted_at IS NULL
                ORDER BY sort_order
                """, Long.class);

        assertThat(ordered).containsExactly(first, second);
    }

    @Test
    void 이력서_항목_순서는_음수를_저장하지_않는다() {
        long cardId = insertCard("card:a");

        assertThatThrownBy(() -> jdbc.update("UPDATE resource_career_cards SET sort_order = -1 WHERE id = ?", cardId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 카드_칸이_없는_테코톡_경험도_경험_조각과_함께_저장한다() {
        long documentId = insertDocument("problem_solving");
        long experienceId = jdbc.queryForObject("""
                INSERT INTO resource_experiences (document_id, experience_index, problem, solution, source)
                VALUES (?, 0, '스크린 리더가 버튼을 읽지 못했어요.', 'aria-label 을 붙였어요.', 'youtube-tekotalk')
                RETURNING id
                """, Long.class, documentId);
        insertChunk(documentId, 1, "experience", experienceId);

        String title = jdbc.queryForObject("SELECT title FROM resource_experiences WHERE id = ?", String.class, experienceId);

        assertThat(title).isNull();
    }

    @Test
    void 다른_문서의_경험을_가리키는_조각은_저장하지_않는다() {
        long documentId = insertDocument("chore");
        long otherExperienceId = insertExperience(insertDocument("chore"), 0);

        assertThatThrownBy(() -> insertChunk(documentId, 1, "experience", otherExperienceId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 경험_하나에_경험_조각을_두_개_저장하지_않는다() {
        long documentId = insertDocument("chore");
        long experienceId = insertExperience(documentId, 0);
        insertChunk(documentId, 1, "experience", experienceId);

        assertThatThrownBy(() -> insertChunk(documentId, 2, "experience", experienceId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 문서_하나에_소개_조각을_두_개_저장하지_않는다() {
        long documentId = insertDocument("chore");
        insertChunk(documentId, 0, "intro", null);

        assertThatThrownBy(() -> insertChunk(documentId, 1, "intro", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_일을_다룬_두_경험을_카드_하나로_묶는다() {
        long prExperienceId = insertExperience(insertDocument("problem_solving"), 0);
        long blogExperienceId = insertExperience(insertDocument("problem_solving"), 0);
        long cardId = insertCard("card:a");
        insertCardSource(cardId, prExperienceId, 0);
        insertCardSource(cardId, blogExperienceId, 1);

        List<Long> sources = jdbc.queryForList(
                "SELECT experience_id FROM resource_career_card_sources WHERE card_id = ? ORDER BY source_order",
                Long.class, cardId);

        assertThat(sources).containsExactly(prExperienceId, blogExperienceId);
    }

    @Test
    void 경험_하나를_두_카드에_넣지_않는다() {
        long experienceId = insertExperience(insertDocument("problem_solving"), 0);
        insertCardSource(insertCard("card:a"), experienceId, 0);
        long otherCardId = insertCard("card:b");

        assertThatThrownBy(() -> insertCardSource(otherCardId, experienceId, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 출처_문서를_지우면_카드의_출처_연결만_지워진다() {
        long documentId = insertDocument("problem_solving");
        long cardId = insertCard("card:a");
        insertCardSource(cardId, insertExperience(documentId, 0), 0);

        jdbc.update("DELETE FROM resource_documents WHERE id = ?", documentId);

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM resource_career_card_sources WHERE card_id = ?", Integer.class, cardId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM resource_career_cards WHERE id = ?", Integer.class, cardId)).isOne();
    }

    private long insertCard(String cardKey) {
        return jdbc.queryForObject("""
                INSERT INTO resource_career_cards (card_key, repo, track, template, title, category, problem,
                                                   solution, resume_bullets, written_by, prompt_version, updated_at)
                VALUES (?, 'woowacourse-teams/2026-shout-outz', 'BE', 'problem_solving', '링크 미리보기 수집 실패 해결',
                        '장애 대응', '운영에서 미리보기가 비었습니다.', 'oEmbed API로 바꿨습니다.',
                        ARRAY['미리보기 실패를 없앰'], 'llm', 'career-v1', now())
                RETURNING id
                """, Long.class, cardKey);
    }

    private void insertCardSource(long cardId, long experienceId, int sourceOrder) {
        jdbc.update("INSERT INTO resource_career_card_sources (card_id, experience_id, source_order) VALUES (?, ?, ?)",
                cardId, experienceId, sourceOrder);
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
