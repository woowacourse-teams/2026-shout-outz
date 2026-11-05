package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.jobposting.application.JobPostingAdminQueryRepository;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingRepository;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk.ChunkKind;
import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSummary;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendation;
import com.shoutoutz.api.jobposting.application.dto.RequirementRecommendationDetail;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingContentHash;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobPostingUrl;
import com.shoutoutz.api.jobposting.domain.JobPostingVisibility;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RecommendationVerdict;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class JobPostingRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-27T01:00:00Z");
    private static final String CONTENT = "자격 요건\n- Java 개발 경험";
    private static final String SOURCE = "github-pr";

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobPostingAdminQueryRepository jobPostingAdminQueryRepository;

    @Autowired
    private JobRequirementMatchingRepository jobRequirementMatchingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private long adminId;

    @BeforeEach
    void setUp() {
        adminId = insertAdmin();
    }

    @Test
    void 공고와_역량_항목을_함께_저장하고_순서대로_조회한다() {
        JobPosting saved = jobPostingRepository.save(posting("https://Careers.Example.com/jobs/1/", NOW));
        // 1차 캐시가 아니라 DB 행을 다시 읽어 칸 타입이 도메인 값으로 돌아오는지 확인한다.
        entityManager.flush();
        entityManager.clear();

        JobPosting found = jobPostingRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSourceUrl().value()).isEqualTo("https://careers.example.com/jobs/1");
        assertThat(found.getContentHash()).isEqualTo(JobPostingContentHash.from(CONTENT));
        assertThat(found.getCompanyName()).isEqualTo("예시 회사");
        assertThat(found.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(found.getCareerRequirement()).isEqualTo("3년 이상");
        assertThat(found.getEmploymentType()).isNull();
        assertThat(found.getDeadline()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(found.getStatus()).isEqualTo(MatchingStatus.PENDING);
        assertThat(found.getRegisteredBy()).isEqualTo(adminId);
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
        assertThat(found.getRequirements())
                .extracting(JobRequirement::getDisplayOrder, JobRequirement::getName, JobRequirement::getCategory,
                        JobRequirement::getStatus)
                .containsExactly(
                        tuple(1, "Java", RequirementCategory.REQUIRED,
                                MatchingStatus.PENDING),
                        tuple(2, "대용량 트래픽 처리", RequirementCategory.PREFERRED,
                                MatchingStatus.PENDING));
        assertThat(found.getRequirements()).allSatisfy(requirement -> assertThat(requirement.getId()).isNotNull());
    }

    @Test
    void 역량_항목의_매칭_작업용_칸은_처음_값으로_저장한다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT attempt_count, lease_until, last_error, created_at FROM job_requirements "
                        + "WHERE job_posting_id = ?",
                saved.getId());

        assertThat(rows).hasSize(2).allSatisfy(row -> {
            assertThat(row.get("attempt_count")).isEqualTo(0);
            assertThat(row.get("lease_until")).isNull();
            assertThat(row.get("last_error")).isNull();
            assertThat(row.get("created_at")).isNotNull();
        });
    }

    @Test
    void 없는_공고는_빈_값을_돌려준다() {
        assertThat(jobPostingRepository.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void 정규화한_주소로_등록된_공고_id를_찾는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));

        assertThat(jobPostingRepository.findIdBySourceUrl(new JobPostingUrl("https://Careers.Example.com/jobs/1/#apply")))
                .contains(saved.getId());
        assertThat(jobPostingRepository.findIdBySourceUrl(new JobPostingUrl("https://careers.example.com/jobs/2")))
                .isEmpty();
    }

    @Test
    void 같은_주소의_공고를_다시_저장하면_중복_예외로_바꾼다() {
        jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));

        assertThatThrownBy(() -> jobPostingRepository.save(posting("https://Careers.Example.com/jobs/1/", NOW)))
                .isInstanceOfSatisfying(DuplicateEntityException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(JobPostingErrorCode.JOB_POSTING_DUPLICATE_URL));
    }

    @Test
    void 공고_목록을_최신_등록순으로_커서를_따라_읽는다() {
        long first = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW)).getId();
        long second = jobPostingRepository.save(posting("https://careers.example.com/jobs/2", NOW.plusSeconds(1))).getId();
        long third = jobPostingRepository.save(posting("https://careers.example.com/jobs/3", NOW.plusSeconds(2))).getId();

        JobPostingPage firstPage = jobPostingAdminQueryRepository.findAll(null, 2);
        JobPostingPage secondPage = jobPostingAdminQueryRepository.findAll(new JobPostingCursor(second), 2);

        assertThat(firstPage.items()).extracting(JobPostingSummary::id).containsExactly(third, second);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.totalCount()).isEqualTo(3);
        assertThat(secondPage.items()).extracting(JobPostingSummary::id).containsExactly(first);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.totalCount()).isEqualTo(3);
    }

    @Test
    void 공고_목록의_한_줄에_목록에_보여_줄_값을_담는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));

        JobPostingSummary summary = jobPostingAdminQueryRepository.findAll(null, 20).items().getFirst();

        assertThat(summary).isEqualTo(new JobPostingSummary(
                saved.getId(), "예시 회사", "백엔드 개발자", JobRole.BACKEND, MatchingStatus.PENDING, 0, null, NOW));
    }

    @Test
    void 공고_목록의_한_줄에_최종_실패한_역량_수만_센다() {
        long failedPosting = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW)).getId();
        long otherPosting = jobPostingRepository.save(posting("https://careers.example.com/jobs/2", NOW.plusSeconds(1))).getId();
        jdbcTemplate.update("UPDATE job_postings SET status = 'FAILED' WHERE id = ?", failedPosting);
        jdbcTemplate.update("UPDATE job_requirements SET status = 'FAILED' WHERE job_posting_id = ? AND display_order = 1",
                failedPosting);
        jdbcTemplate.update("""
                UPDATE job_requirements SET status = 'PENDING', last_error = 'SEARCH_FAILED'
                WHERE job_posting_id = ? AND display_order = 2
                """, failedPosting);
        jdbcTemplate.update("UPDATE job_requirements SET status = 'FAILED' WHERE job_posting_id = ?", otherPosting);

        List<JobPostingSummary> items = jobPostingAdminQueryRepository.findAll(null, 20).items();

        assertThat(items).extracting(JobPostingSummary::id, JobPostingSummary::failedRequirementCount)
                .containsExactly(tuple(otherPosting, 2), tuple(failedPosting, 1));
    }

    @Test
    void 숨긴_공고는_목록의_한_줄에_숨긴_시각을_담는다() {
        long id = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW)).getId();
        jobPostingRepository.updateHiddenAt(id, true, NOW.plusSeconds(60));

        JobPostingSummary summary = jobPostingAdminQueryRepository.findAll(null, 20).items().getFirst();

        assertThat(summary.hiddenAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void 숨기면_숨김_시각을_채우고_다시_숨겨도_처음_시각을_둔다() {
        long id = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW)).getId();

        JobPostingVisibility first = jobPostingRepository.updateHiddenAt(id, true, NOW.plusSeconds(60)).orElseThrow();
        JobPostingVisibility second = jobPostingRepository.updateHiddenAt(id, true, NOW.plusSeconds(120)).orElseThrow();

        assertThat(first).isEqualTo(new JobPostingVisibility(id, NOW.plusSeconds(60)));
        assertThat(second).isEqualTo(new JobPostingVisibility(id, NOW.plusSeconds(60)));
        assertThat(second.hidden()).isTrue();
    }

    @Test
    void 숨김을_해제하면_숨김_시각을_비우고_숨기지_않은_공고를_해제해도_그대로다() {
        long id = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW)).getId();
        jobPostingRepository.updateHiddenAt(id, true, NOW.plusSeconds(60));

        JobPostingVisibility shown = jobPostingRepository.updateHiddenAt(id, false, NOW.plusSeconds(120)).orElseThrow();
        JobPostingVisibility shownAgain = jobPostingRepository.updateHiddenAt(id, false, NOW.plusSeconds(180)).orElseThrow();

        assertThat(shown).isEqualTo(new JobPostingVisibility(id, null));
        assertThat(shownAgain.hidden()).isFalse();
    }

    @Test
    void 숨김은_공고의_분석_상태와_역량의_매칭_칸을_바꾸지_않는다() {
        long id = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW)).getId();
        // Worker 가 SQL 로 바꿔 둔 값이 숨김으로 덮어써지지 않는지 본다.
        jdbcTemplate.update("UPDATE job_postings SET status = 'PROCESSING' WHERE id = ?", id);
        jdbcTemplate.update("""
                UPDATE job_requirements
                SET status = 'PROCESSING', attempt_count = 2, lease_until = ?, last_error = 'SEARCH_FAILED'
                WHERE job_posting_id = ?
                """, Timestamp.from(NOW.plusSeconds(180)), id);

        jobPostingRepository.updateHiddenAt(id, true, NOW.plusSeconds(60));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM job_postings WHERE id = ?", String.class, id))
                .isEqualTo("PROCESSING");
        assertThat(jdbcTemplate.queryForList(
                "SELECT status, attempt_count, lease_until, last_error FROM job_requirements WHERE job_posting_id = ?",
                id)).hasSize(2).allSatisfy(row -> {
            assertThat(row.get("status")).isEqualTo("PROCESSING");
            assertThat(row.get("attempt_count")).isEqualTo(2);
            assertThat(row.get("lease_until")).isNotNull();
            assertThat(row.get("last_error")).isEqualTo("SEARCH_FAILED");
        });
    }

    @Test
    void 없는_공고의_숨김을_바꾸면_빈_값을_돌려준다() {
        assertThat(jobPostingRepository.updateHiddenAt(Long.MAX_VALUE, true, NOW)).isEmpty();
    }

    @Test
    void 공고를_지우면_역량과_추천_자료도_함께_지워진다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        long requirementId = saved.getRequirements().getFirst().getId();
        jdbcTemplate.update("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, evidence, similarity_score, created_at)
                VALUES (?, 'GITHUB_PR', 'pr-1', 'MATCH', '판정 이유', '[]'::jsonb, 0.5, now())
                """, requirementId);

        boolean deleted = jobPostingRepository.deleteById(saved.getId());

        assertThat(deleted).isTrue();
        // SQL 로 지워서 같은 트랜잭션의 1차 캐시에는 공고가 남아 있으므로 DB 행을 직접 센다.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM job_postings WHERE id = ?", Integer.class, saved.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM job_requirements WHERE job_posting_id = ?", Integer.class, saved.getId()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM requirement_recommendations WHERE job_requirement_id = ?", Integer.class,
                requirementId)).isZero();
    }

    @Test
    void 없는_공고를_지우면_false_를_돌려준다() {
        assertThat(jobPostingRepository.deleteById(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void 원문이_바뀌면_옛_역량과_추천_자료를_지우고_새_역량을_대기로_넣는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        long oldRequirementId = saved.getRequirements().getFirst().getId();
        insertRecommendation(oldRequirementId);
        jdbcTemplate.update("UPDATE job_postings SET status = 'READY' WHERE id = ?", saved.getId());
        jdbcTemplate.update("UPDATE job_requirements SET status = 'READY', attempt_count = 1 WHERE job_posting_id = ?",
                saved.getId());
        jobPostingRepository.updateHiddenAt(saved.getId(), true, NOW.plusSeconds(30));
        clearPersistenceContext();

        JobPosting locked = jobPostingRepository.findByIdForUpdate(saved.getId()).orElseThrow();
        JobPosting updated = locked.update(
                "https://careers.example.com/jobs/1", CONTENT + "\n- Kotlin 개발 경험", "예시 회사", JobRole.BACKEND,
                "3년 이상", "백엔드 개발자", null, LocalDate.of(2026, 12, 31),
                List.of(JobRequirement.create(1, "Kotlin", RequirementCategory.REQUIRED, "Kotlin 개발 경험")),
                NOW.plusSeconds(60));
        jobPostingRepository.update(updated, true);
        clearPersistenceContext();

        JobPosting found = jobPostingRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(MatchingStatus.PENDING);
        assertThat(found.getContentHash()).isEqualTo(JobPostingContentHash.from(CONTENT + "\n- Kotlin 개발 경험"));
        assertThat(found.getDeadline()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(found.getHiddenAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(found.getRequirements())
                .extracting(JobRequirement::getName, JobRequirement::getStatus)
                .containsExactly(tuple("Kotlin", MatchingStatus.PENDING));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM requirement_recommendations WHERE job_requirement_id = ?", Integer.class,
                oldRequirementId)).isZero();
        assertThat(jdbcTemplate.queryForMap(
                "SELECT attempt_count, created_at FROM job_requirements WHERE job_posting_id = ?", saved.getId()))
                .satisfies(row -> {
                    assertThat(row.get("attempt_count")).isEqualTo(0);
                    assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(NOW.plusSeconds(60));
                });
    }

    @Test
    void 원문이_같으면_공고_정보만_바꾸고_역량과_추천_자료와_매칭_칸을_그대로_둔다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        List<Long> requirementIds = saved.getRequirements().stream().map(JobRequirement::getId).toList();
        insertRecommendation(requirementIds.getFirst());
        jdbcTemplate.update("UPDATE job_postings SET status = 'READY' WHERE id = ?", saved.getId());
        jdbcTemplate.update("UPDATE job_requirements SET status = 'READY', attempt_count = 2 WHERE job_posting_id = ?",
                saved.getId());
        clearPersistenceContext();

        JobPosting locked = jobPostingRepository.findByIdForUpdate(saved.getId()).orElseThrow();
        JobPosting updated = locked.update(
                "https://careers.example.com/jobs/1", CONTENT, "바뀐 회사", JobRole.BACKEND, "3년 이상", "백엔드 개발자",
                null, LocalDate.of(2026, 12, 31), List.of(), NOW.plusSeconds(60));
        jobPostingRepository.update(updated, false);
        clearPersistenceContext();

        JobPosting found = jobPostingRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCompanyName()).isEqualTo("바뀐 회사");
        assertThat(found.getDeadline()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(found.getStatus()).isEqualTo(MatchingStatus.READY);
        assertThat(found.getRequirements()).extracting(JobRequirement::getId).isEqualTo(requirementIds);
        assertThat(jdbcTemplate.queryForList(
                "SELECT attempt_count FROM job_requirements WHERE job_posting_id = ?", Integer.class, saved.getId()))
                .containsOnly(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM requirement_recommendations WHERE job_requirement_id = ?", Integer.class,
                requirementIds.getFirst())).isOne();
    }

    @Test
    void 없는_공고를_잠그고_읽으면_빈_값을_돌려준다() {
        assertThat(jobPostingRepository.findByIdForUpdate(Long.MAX_VALUE)).isEmpty();
    }

    /**
     * SQL 로 바꾼 값을 JPA 가 1차 캐시의 옛 값으로 읽지 않도록 비운다.
     * 실제 요청은 트랜잭션마다 캐시가 비어 있어서 처음 읽을 때 DB 값을 본다.
     */
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private void insertRecommendation(long requirementId) {
        jdbcTemplate.update("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, evidence, similarity_score, created_at)
                VALUES (?, 'GITHUB_PR', 'pr-1', 'MATCH', '판정 이유', '[]'::jsonb, 0.5, now())
                """, requirementId);
    }

    @Test
    void 추천_자료를_등급과_검색_점수_순으로_읽고_NOT_MATCH도_포함한다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        long requirementId = saved.getRequirements().getFirst().getId();
        insertRecommendation(requirementId, "not-match", "NOT_MATCH", null, 0.9);
        insertRecommendation(requirementId, "partial", "PARTIAL", "설명", 0.8);
        insertRecommendation(requirementId, "match-low", "MATCH", "설명", 0.5);
        insertRecommendation(requirementId, "match-high", "MATCH", "설명", 0.7);

        List<RequirementRecommendationDetail> found = jobPostingAdminQueryRepository.findRecommendations(saved.getId());

        assertThat(found).extracting(RequirementRecommendationDetail::externalKey)
                .containsExactly("match-high", "match-low", "partial", "not-match");
        assertThat(found.getLast().verdict()).isEqualTo(RecommendationVerdict.NOT_MATCH);
        assertThat(found.getLast().description()).isNull();
    }

    @Test
    void 추천_자료에_자료의_제목과_링크_판정_근거를_담는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        long requirementId = saved.getRequirements().getFirst().getId();
        insertDocument("resource-1", null);
        insertRecommendation(requirementId, "resource-1", "MATCH", "과정을 볼 수 있어요", 0.7);

        List<RequirementRecommendationDetail> found = jobPostingAdminQueryRepository.findRecommendations(saved.getId());

        assertThat(found).containsExactly(new RequirementRecommendationDetail(
                requirementId, SOURCE, "resource-1", "자료 제목", "https://example.com/resource-1",
                RecommendationVerdict.MATCH, "판정 이유", "과정을 볼 수 있어요", 0.7,
                List.of(new CandidateChunk(ChunkKind.EXPERIENCE, "경험 조각", 0.7))));
    }

    @Test
    void 자료가_삭제됐거나_없으면_제목과_링크는_비우고_판정_내용은_그대로_읽는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        long requirementId = saved.getRequirements().getFirst().getId();
        insertDocument("deleted", NOW);
        insertRecommendation(requirementId, "deleted", "MATCH", "설명", 0.7);
        insertRecommendation(requirementId, "missing", "MATCH", "설명", 0.6);

        List<RequirementRecommendationDetail> found = jobPostingAdminQueryRepository.findRecommendations(saved.getId());

        assertThat(found).hasSize(2).allSatisfy(recommendation -> {
            assertThat(recommendation.title()).isNull();
            assertThat(recommendation.url()).isNull();
            assertThat(recommendation.reason()).isEqualTo("판정 이유");
        });
    }

    @Test
    void 키가_같아도_출처가_다른_자료의_제목과_링크는_붙이지_않는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        long requirementId = saved.getRequirements().getFirst().getId();
        insertDocument("resource-1", null);
        jdbcTemplate.update("UPDATE resource_documents SET source = 'other-source' WHERE external_key = 'resource-1'");
        insertRecommendation(requirementId, "resource-1", "MATCH", "설명", 0.7);

        List<RequirementRecommendationDetail> found = jobPostingAdminQueryRepository.findRecommendations(saved.getId());

        assertThat(found).singleElement().satisfies(recommendation -> {
            assertThat(recommendation.title()).isNull();
            assertThat(recommendation.url()).isNull();
        });
    }

    @Test
    void 역량_완료_때_저장한_판정_근거를_그대로_읽는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        JobRequirement requirement = saved.getRequirements().getFirst();
        jdbcTemplate.update("UPDATE job_requirements SET status = 'PROCESSING', attempt_count = 1 WHERE id = ?",
                requirement.getId());
        List<CandidateChunk> evidence = List.of(
                new CandidateChunk(ChunkKind.EXPERIENCE, "따옴표 \" 와\n줄바꿈이 있는 조각", 0.0001),
                new CandidateChunk(ChunkKind.INTRO, "소개 조각", 0.5));
        ClaimedJobRequirement claimed = new ClaimedJobRequirement(requirement.getId(), saved.getId(), 1,
                JobRole.BACKEND, requirement.getName(), requirement.getCategory(), requirement.getSummary());

        jobRequirementMatchingRepository.complete(claimed, List.of(new RequirementRecommendation(
                SOURCE, "resource-1", RecommendationVerdict.MATCH, "판정 이유", "설명", 0.7, evidence)), NOW);

        assertThat(jobPostingAdminQueryRepository.findRecommendations(saved.getId()))
                .singleElement()
                .satisfies(recommendation -> assertThat(recommendation.evidence()).isEqualTo(evidence));
    }

    @Test
    void 공고의_모든_역량_추천을_읽고_다른_공고의_추천은_읽지_않는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        JobPosting other = jobPostingRepository.save(posting("https://careers.example.com/jobs/2", NOW));
        long firstId = saved.getRequirements().get(0).getId();
        long secondId = saved.getRequirements().get(1).getId();
        insertRecommendation(firstId, "first", "PARTIAL", "설명", 0.5);
        insertRecommendation(secondId, "second", "MATCH", "설명", 0.9);
        insertRecommendation(other.getRequirements().getFirst().getId(), "other", "MATCH", "설명", 0.9);

        List<RequirementRecommendationDetail> found = jobPostingAdminQueryRepository.findRecommendations(saved.getId());

        assertThat(found)
                .extracting(RequirementRecommendationDetail::requirementId, RequirementRecommendationDetail::externalKey)
                .containsExactly(tuple(firstId, "first"), tuple(secondId, "second"));
    }

    @Test
    void 마지막_실패_이유가_남은_역량만_읽고_다른_공고의_역량은_읽지_않는다() {
        JobPosting saved = jobPostingRepository.save(posting("https://careers.example.com/jobs/1", NOW));
        JobPosting other = jobPostingRepository.save(posting("https://careers.example.com/jobs/2", NOW));
        long failedId = saved.getRequirements().getFirst().getId();
        jdbcTemplate.update("UPDATE job_requirements SET last_error = 'JUDGE_FAILED' WHERE id = ?", failedId);
        jdbcTemplate.update("UPDATE job_requirements SET last_error = 'SEARCH_FAILED' WHERE id = ?",
                other.getRequirements().getFirst().getId());

        assertThat(jobPostingAdminQueryRepository.findLastErrors(saved.getId()))
                .containsExactly(Map.entry(failedId, MatchingFailure.JUDGE_FAILED));
    }

    private void insertDocument(String externalKey, Instant deletedAt) {
        Timestamp deleted = null;
        if (deletedAt != null) {
            deleted = Timestamp.from(deletedAt);
        }
        jdbcTemplate.update("""
                INSERT INTO resource_documents (source, external_key, version, title, url, prompt_version, updated_at,
                                                deleted_at)
                VALUES (?, ?, 1, '자료 제목', ?, 'test-v1', now(), ?)
                """, SOURCE, externalKey, "https://example.com/" + externalKey, deleted);
    }

    private void insertRecommendation(long requirementId, String externalKey, String verdict, String description,
                                      double similarityScore) {
        String evidence = """
                [{"kind": "EXPERIENCE", "text": "경험 조각", "similarityScore": 0.7}]
                """;
        jdbcTemplate.update("""
                INSERT INTO requirement_recommendations (job_requirement_id, resource_source, resource_external_key,
                                                         verdict, reason, description, evidence, similarity_score,
                                                         created_at)
                VALUES (?, ?, ?, ?, '판정 이유', ?, CAST(? AS jsonb), ?, now())
                """, requirementId, SOURCE, externalKey, verdict, description, evidence, similarityScore);
    }

    private JobPosting posting(String url, Instant now) {
        return JobPosting.register(
                url,
                CONTENT,
                "예시 회사",
                JobRole.BACKEND,
                "3년 이상",
                "백엔드 개발자",
                null,
                LocalDate.of(2026, 11, 30),
                adminId,
                List.of(
                        JobRequirement.create(1, "Java", RequirementCategory.REQUIRED, "Java로 서버를 개발해 본 경험"),
                        JobRequirement.create(2, "대용량 트래픽 처리", RequirementCategory.PREFERRED, "트래픽이 많은 서비스 운영 경험")),
                now);
    }

    private long insertAdmin() {
        String handle = "job-posting-admin-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle, role) VALUES (?, 'ADMIN') RETURNING id",
                Long.class,
                handle
        );
    }
}
