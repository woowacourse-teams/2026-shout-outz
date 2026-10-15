package com.shoutoutz.api.interview;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.interview.application.InterviewAccessService;
import com.shoutoutz.api.interview.application.InterviewQueryService;
import com.shoutoutz.api.interview.application.InterviewViewService;
import com.shoutoutz.api.interview.domain.InterviewViewRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class InterviewViewIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired InterviewViewService views;
    @Autowired InterviewAccessService access;
    @Autowired InterviewViewRepository repository;
    @Autowired InterviewQueryService queryService;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    long userId;
    long interviewId;

    @BeforeEach
    void setUp() {
        userId = jdbc.queryForObject("INSERT INTO users(handle, role) VALUES (?, 'ADMIN') RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        interviewId = jdbc.queryForObject("""
                INSERT INTO interviews(title, interviewee_id, author_id, status, published_at)
                VALUES ('조회 검증', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, userId, userId);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM interviews WHERE id = ?", interviewId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void 같은_방문자의_조회는_한국_날짜가_바뀌면_다시_집계한다() {
        VisitorKey visitor = new VisitorKey("a".repeat(64));
        assertThat(recordAt(visitor, "2026-10-15T14:59:59Z")).isEqualTo(1);
        assertThat(recordAt(visitor, "2026-10-15T14:59:59Z")).isEqualTo(1);
        assertThat(recordAt(visitor, "2026-10-15T15:00:00Z")).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT viewed_on::text FROM interview_view_days WHERE interview_id = ? ORDER BY viewed_on",
                String.class, interviewId)).containsExactly("2026-10-15", "2026-10-16");
        assertThat(jdbc.queryForList("SELECT visitor_key_hash FROM interview_view_days WHERE interview_id = ?",
                String.class, interviewId)).containsOnly(visitor.hash());
    }

    @Test
    void 같은_방문자가_동시에_요청해도_조회수는_한번만_오른다() throws Exception {
        var updatedAt = jdbc.queryForObject("SELECT updated_at FROM interviews WHERE id = ?", Instant.class, interviewId);
        VisitorKey visitor = new VisitorKey("b".repeat(64));
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> views.record(interviewId, visitor).viewCount());
        }
        assertThat(run(tasks)).containsOnly(1L);
        assertThat(count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM interviews WHERE id = ?", Instant.class, interviewId))
                .isEqualTo(updatedAt);
    }

    @Test
    void 서로_다른_방문자의_동시_조회는_모두_집계한다() throws Exception {
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            VisitorKey visitor = new VisitorKey("visitor-" + i);
            tasks.add(() -> views.record(interviewId, visitor).viewCount());
        }
        run(tasks);
        assertThat(count()).isEqualTo(8);
    }

    @Test
    void 관리자_원본_조회와_공개_상세_조회는_집계하지_않는다() {
        queryService.findAdmin(interviewId, userId);
        queryService.find(interviewId);
        assertThat(count()).isZero();
    }

    private long recordAt(VisitorKey visitor, String timestamp) {
        var service = new InterviewViewService(access, repository,
                Clock.fixed(Instant.parse(timestamp), ZoneOffset.UTC));
        return new TransactionTemplate(transactionManager)
                .execute(status -> service.record(interviewId, visitor).viewCount());
    }

    private long count() {
        return jdbc.queryForObject("SELECT view_count FROM interviews WHERE id = ?", Long.class, interviewId);
    }

    private List<Long> run(List<Callable<Long>> tasks) throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Long> results = new ArrayList<>();
            for (var future : executor.invokeAll(tasks, 15, TimeUnit.SECONDS)) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
