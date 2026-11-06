package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.application.JobPostingQueryRepository;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListCursor;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListItem;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListPage;
import com.shoutoutz.api.jobposting.application.dto.JobPostingSearchCondition;
import com.shoutoutz.api.jobposting.domain.JobPosting;
import com.shoutoutz.api.jobposting.domain.JobPostingRepository;
import com.shoutoutz.api.jobposting.domain.JobRequirement;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class JobPostingQueryRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-01T01:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

    @Autowired
    private JobPostingQueryRepository jobPostingQueryRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long adminId;
    private int urlSequence;

    @BeforeEach
    void setUp() {
        adminId = insertAdmin();
    }

    @Test
    void 분석이_끝났고_숨기지_않았고_마감일이_없는_공고를_읽는다() {
        long id = visiblePosting(NOW);

        assertThat(ids(findAll(null, null, null, 20))).containsExactly(id);
    }

    @ParameterizedTest
    @EnumSource(value = MatchingStatus.class, names = "READY", mode = EnumSource.Mode.EXCLUDE)
    void 분석이_끝나지_않은_공고는_읽지_않는다(MatchingStatus status) {
        long id = save(posting(JobRole.BACKEND, "예시 회사", "백엔드 개발자", null, List.of("Java"), NOW));
        updateStatus(id, status);

        assertThat(findAll(null, null, null, 20).items()).isEmpty();
    }

    @Test
    void 숨긴_공고는_읽지_않는다() {
        long id = visiblePosting(NOW);
        jobPostingRepository.updateHiddenAt(id, true, NOW);

        assertThat(findAll(null, null, null, 20).items()).isEmpty();
    }

    @Test
    void 마감일이_오늘이거나_이후인_공고는_읽고_마감일이_지난_공고는_읽지_않는다() {
        long onDeadline = visiblePosting(posting(JobRole.BACKEND, "예시 회사", "오늘 마감", TODAY, List.of("Java"), NOW));
        long beforeDeadline = visiblePosting(
                posting(JobRole.BACKEND, "예시 회사", "내일 마감", TODAY.plusDays(1), List.of("Java"), NOW));
        visiblePosting(posting(JobRole.BACKEND, "예시 회사", "어제 마감", TODAY.minusDays(1), List.of("Java"), NOW));

        assertThat(ids(findAll(null, null, null, 20))).containsExactlyInAnyOrder(onDeadline, beforeDeadline);
    }

    @Test
    void 최신_등록순으로_읽고_등록_시각이_같으면_ID가_큰_공고부터_읽는다() {
        long oldest = visiblePosting(NOW);
        long sameTimeFirst = visiblePosting(NOW.plusSeconds(10));
        long sameTimeSecond = visiblePosting(NOW.plusSeconds(10));

        assertThat(ids(findAll(null, null, null, 20))).containsExactly(sameTimeSecond, sameTimeFirst, oldest);
    }

    @Test
    void 커서_다음_공고부터_읽고_더_읽을_공고가_있는지_알려_준다() {
        long first = visiblePosting(NOW);
        long second = visiblePosting(NOW.plusSeconds(1));
        long third = visiblePosting(NOW.plusSeconds(2));

        JobPostingListPage firstPage = findAll(null, null, null, 2);
        JobPostingListItem last = firstPage.items().getLast();
        JobPostingListPage secondPage = findAll(null, null,
                JobPostingListCursor.latest(last.createdAt(), last.jobPostingId()), 2);

        assertThat(ids(firstPage)).containsExactly(third, second);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(ids(secondPage)).containsExactly(first);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 등록_시각이_같은_공고가_페이지_경계에_걸려도_빠지거나_겹치지_않는다() {
        long older = visiblePosting(NOW);
        long sameTimeFirst = visiblePosting(NOW.plusSeconds(10));
        long sameTimeSecond = visiblePosting(NOW.plusSeconds(10));

        JobPostingListPage firstPage = findAll(null, null, null, 1);
        JobPostingListPage secondPage = findAll(null, null, nextCursor(firstPage), 1);
        JobPostingListPage thirdPage = findAll(null, null, nextCursor(secondPage), 1);

        assertThat(ids(firstPage)).containsExactly(sameTimeSecond);
        assertThat(ids(secondPage)).containsExactly(sameTimeFirst);
        assertThat(ids(thirdPage)).containsExactly(older);
        assertThat(thirdPage.hasNext()).isFalse();
    }

    @Test
    void 남은_공고가_요청한_개수와_같으면_더_읽을_공고가_없다고_알려_준다() {
        visiblePosting(NOW);
        visiblePosting(NOW.plusSeconds(1));

        JobPostingListPage page = findAll(null, null, null, 2);

        assertThat(page.items()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void 직군을_고르면_그_직군의_공고만_읽는다() {
        long backend = visiblePosting(posting(JobRole.BACKEND, "예시 회사", "백엔드 개발자", null, List.of("Java"), NOW));
        visiblePosting(posting(JobRole.FRONTEND, "예시 회사", "프론트엔드 개발자", null, List.of("React"), NOW));

        assertThat(ids(findAll(JobRole.BACKEND, null, null, 20))).containsExactly(backend);
    }

    @ParameterizedTest
    @ValueSource(strings = {"토스", "server", "KOTLIN"})
    void 회사_이름_공고_제목_역량_이름_중_하나에_검색어가_있으면_읽고_대소문자는_가리지_않는다(String keyword) {
        long matched = visiblePosting(
                posting(JobRole.BACKEND, "토스뱅크", "Server Developer", null, List.of("Kotlin, Spring"), NOW));
        visiblePosting(posting(JobRole.BACKEND, "예시 회사", "백엔드 개발자", null, List.of("Java"), NOW));

        assertThat(ids(findAll(null, keyword, null, 20))).containsExactly(matched);
    }

    @Test
    void 검색어의_퍼센트와_밑줄은_일반_글자로_찾는다() {
        long matched = visiblePosting(posting(JobRole.BACKEND, "예시 회사", "성장률 100% 팀", null, List.of("Java"), NOW));
        visiblePosting(posting(JobRole.BACKEND, "예시 회사", "성장률 1000 팀", null, List.of("Java"), NOW));

        assertThat(ids(findAll(null, "100%", null, 20))).containsExactly(matched);
    }

    @Test
    void 공고_한_줄에_공고_정보와_역량_이름_앞_3개와_전체_역량_수를_담는다() {
        JobPosting posting = posting(JobRole.BACKEND, "토스뱅크", "Server Developer", LocalDate.of(2026, 11, 30),
                List.of("Kotlin, Spring", "데이터 모델링", "대용량 트래픽 처리", "Redis 캐시", "Kafka"), NOW);
        long id = visiblePosting(posting);

        JobPostingListItem item = findAll(null, null, null, 20).items().getFirst();

        assertThat(item).isEqualTo(new JobPostingListItem(
                id, "토스뱅크", JobRole.BACKEND, "정규직", "3년 이상", "Server Developer", LocalDate.of(2026, 11, 30),
                5, List.of("Kotlin, Spring", "데이터 모델링", "대용량 트래픽 처리"), NOW));
    }

    @Test
    void 직군별_공고_수는_노출_조건과_검색어를_반영하고_직군_조건은_걸지_않는다() {
        visiblePosting(posting(JobRole.BACKEND, "토스뱅크", "백엔드 개발자", null, List.of("Java"), NOW));
        visiblePosting(posting(JobRole.BACKEND, "토스증권", "백엔드 개발자", null, List.of("Java"), NOW));
        visiblePosting(posting(JobRole.FRONTEND, "토스증권", "프론트엔드 개발자", null, List.of("React"), NOW));
        visiblePosting(posting(JobRole.ANDROID, "예시 회사", "안드로이드 개발자", null, List.of("Kotlin"), NOW));
        long hidden = visiblePosting(posting(JobRole.FRONTEND, "토스페이", "프론트엔드 개발자", null, List.of("React"), NOW));
        jobPostingRepository.updateHiddenAt(hidden, true, NOW);

        Map<JobRole, Long> counts = jobPostingQueryRepository.countByJobRole("토스", TODAY);

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(JobRole.BACKEND, 2L, JobRole.FRONTEND, 1L));
    }

    @Test
    void 검색어가_없으면_보여_줄_공고를_모두_직군별로_센다() {
        visiblePosting(posting(JobRole.BACKEND, "토스뱅크", "백엔드 개발자", null, List.of("Java"), NOW));
        visiblePosting(posting(JobRole.ANDROID, "예시 회사", "안드로이드 개발자", null, List.of("Kotlin"), NOW));

        Map<JobRole, Long> counts = jobPostingQueryRepository.countByJobRole(null, TODAY);

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(JobRole.BACKEND, 1L, JobRole.ANDROID, 1L));
    }

    private JobPostingListPage findAll(JobRole jobRole, String keyword, JobPostingListCursor cursor, int size) {
        return jobPostingQueryRepository.findAll(new JobPostingSearchCondition(jobRole, keyword, TODAY, cursor, size));
    }

    private JobPostingListCursor nextCursor(JobPostingListPage page) {
        JobPostingListItem last = page.items().getLast();
        return JobPostingListCursor.latest(last.createdAt(), last.jobPostingId());
    }

    private List<Long> ids(JobPostingListPage page) {
        return page.items().stream().map(JobPostingListItem::jobPostingId).toList();
    }

    private long visiblePosting(Instant createdAt) {
        return visiblePosting(posting(JobRole.BACKEND, "예시 회사", "백엔드 개발자", null, List.of("Java"), createdAt));
    }

    private long visiblePosting(JobPosting posting) {
        long id = save(posting);
        updateStatus(id, MatchingStatus.READY);
        return id;
    }

    private long save(JobPosting posting) {
        return jobPostingRepository.save(posting).getId();
    }

    private void updateStatus(long id, MatchingStatus status) {
        jdbcTemplate.update("UPDATE job_postings SET status = ? WHERE id = ?", status.name(), id);
    }

    private JobPosting posting(JobRole jobRole, String companyName, String title, LocalDate deadline,
                               List<String> requirementNames, Instant createdAt) {
        List<JobRequirement> requirements = IntStream.range(0, requirementNames.size())
                .mapToObj(index -> JobRequirement.create(
                        index + 1, requirementNames.get(index), RequirementCategory.REQUIRED, "역량 요약"))
                .toList();
        return JobPosting.register(
                "https://careers.example.com/jobs/" + (++urlSequence),
                "공고 원문 " + urlSequence,
                companyName,
                jobRole,
                "3년 이상",
                title,
                "정규직",
                deadline,
                adminId,
                requirements,
                createdAt);
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
