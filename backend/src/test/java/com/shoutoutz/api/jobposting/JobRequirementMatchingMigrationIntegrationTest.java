package com.shoutoutz.api.jobposting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class JobRequirementMatchingMigrationIntegrationTest {

    private static final String EVIDENCE = """
            [{"chunkKind": "experience", "text": "문제: 워커가 중단되면 작업을 잃었다.", "similarityScore": 0.91}]
            """;

    @Autowired
    private JdbcTemplate jdbc;

    private long requirementId;

    @BeforeEach
    void setUp() {
        long userId = jdbc.queryForObject(
                "INSERT INTO users (handle) VALUES ('matching-migration') RETURNING id", Long.class);
        long postingId = jdbc.queryForObject("""
                INSERT INTO job_postings (source_url, content_hash, company_name, job_role, title, status,
                                          registered_by, created_at, updated_at)
                VALUES ('https://example.com/jobs/1', repeat('a', 64), '우테코', 'BACKEND', '백엔드 개발자', 'PENDING',
                        ?, now(), now())
                RETURNING id
                """, Long.class, userId);
        requirementId = jdbc.queryForObject("""
                INSERT INTO job_requirements (job_posting_id, display_order, name, category, summary, status,
                                              created_at, updated_at)
                VALUES (?, 1, 'Spring', 'REQUIRED', 'Spring 기반 서비스를 개발한 경험을 요구해요.', 'PENDING', now(), now())
                RETURNING id
                """, Long.class, postingId);
    }

    @Test
    void 판정한_자료와_판정_근거를_jsonb로_저장한다() {
        insertRecommendation("github-pr", "team/repo#1", "MATCH");

        String chunkKind = jdbc.queryForObject("""
                SELECT evidence -> 0 ->> 'chunkKind' FROM requirement_recommendations WHERE job_requirement_id = ?
                """, String.class, requirementId);

        assertThat(chunkKind).isEqualTo("experience");
    }

    @Test
    void 같은_역량에_같은_자료를_두_번_저장하지_않는다() {
        insertRecommendation("github-pr", "team/repo#1", "MATCH");

        assertThatThrownBy(() -> insertRecommendation("github-pr", "team/repo#1", "PARTIAL"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 원천이_다르면_같은_원본_키도_저장한다() {
        insertRecommendation("github-pr", "team/repo#1", "MATCH");
        insertRecommendation("github-wiki", "team/repo#1", "PARTIAL");

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM requirement_recommendations WHERE job_requirement_id = ?",
                Integer.class, requirementId);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void 정해진_등급이_아니면_저장하지_않는다() {
        assertThatThrownBy(() -> insertRecommendation("github-pr", "team/repo#1", "UNKNOWN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 역량을_지우면_추천_자료도_함께_지워진다() {
        insertRecommendation("github-pr", "team/repo#1", "MATCH");

        jdbc.update("DELETE FROM job_requirements WHERE id = ?", requirementId);

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM requirement_recommendations WHERE job_requirement_id = ?",
                Integer.class, requirementId);
        assertThat(count).isZero();
    }

    @Test
    void 역량에_다음_시도_시각을_저장한다() {
        jdbc.update("UPDATE job_requirements SET next_attempt_at = now() + interval '1 minute' WHERE id = ?",
                requirementId);

        Boolean scheduled = jdbc.queryForObject(
                "SELECT next_attempt_at > now() FROM job_requirements WHERE id = ?", Boolean.class, requirementId);

        assertThat(scheduled).isTrue();
    }

    @Test
    void 대기와_분석_중_역량만_담는_인덱스가_있다() {
        List<String> definitions = jdbc.queryForList("""
                SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_job_requirements_matching_queue'
                """, String.class);

        assertThat(definitions).singleElement().asString()
                .contains("WHERE")
                .contains("PENDING")
                .contains("PROCESSING");
    }

    private void insertRecommendation(String source, String externalKey, String verdict) {
        jdbc.update("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, evidence, similarity_score, created_at)
                VALUES (?, ?, ?, ?, '분산 락을 적용한 경험이 있어요.', ?::jsonb, 0.91, now())
                """, requirementId, source, externalKey, verdict, EVIDENCE);
    }
}
