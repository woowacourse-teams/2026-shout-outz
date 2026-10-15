package com.shoutoutz.api.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.interview.application.InterviewService;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class InterviewViewIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InterviewService interviewService;

    @MockitoBean
    private Clock clock;

    private long userId;
    private long interviewId;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-10-15T12:00:00Z"));
        userId = jdbcTemplate.queryForObject("INSERT INTO users(handle, role) VALUES (?, 'ADMIN') RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        interviewId = jdbcTemplate.queryForObject("""
                INSERT INTO interviews(title, interviewee_id, author_id, status, published_at)
                VALUES ('조회 검증', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, userId, userId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM interviews WHERE id = ?", interviewId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void 같은_방문자의_조회는_한국_날짜가_바뀌면_다시_집계한다() {
        VisitorKey visitor = new VisitorKey("a".repeat(64));
        assertThat(recordAt(visitor, "2026-10-15T14:59:59Z")).isEqualTo(1);
        assertThat(recordAt(visitor, "2026-10-15T14:59:59Z")).isEqualTo(1);
        assertThat(recordAt(visitor, "2026-10-15T15:00:00Z")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("SELECT viewed_on::text FROM interview_view_days WHERE interview_id = ? ORDER BY viewed_on",
                String.class, interviewId)).containsExactly("2026-10-15", "2026-10-16");
        assertThat(jdbcTemplate.queryForList("SELECT visitor_key_hash FROM interview_view_days WHERE interview_id = ?",
                String.class, interviewId)).containsOnly(visitor.hash());
    }

    @Test
    void 같은_방문자가_동시에_요청해도_조회수는_한번만_오른다() throws Exception {
        var updatedAt = jdbcTemplate.queryForObject("SELECT updated_at FROM interviews WHERE id = ?", Instant.class, interviewId);
        VisitorKey visitor = new VisitorKey("b".repeat(64));
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> interviewService.find(interviewId, null, visitor).viewCount());
        }
        assertThat(run(tasks)).containsOnly(1L);
        assertThat(count()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT updated_at FROM interviews WHERE id = ?", Instant.class, interviewId))
                .isEqualTo(updatedAt);
    }

    @Test
    void 서로_다른_방문자의_동시_조회는_모두_집계한다() throws Exception {
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            VisitorKey visitor = new VisitorKey("visitor-" + i);
            tasks.add(() -> interviewService.find(interviewId, null, visitor).viewCount());
        }
        run(tasks);
        assertThat(count()).isEqualTo(8);
    }

    @Test
    void 관리자_원본과_목록_조회는_집계하지_않는다() {
        interviewService.findAdmin(interviewId, userId);
        interviewService.findAll(null, 20);
        interviewService.findAllAdmin(userId, null, 20);
        assertThat(count()).isZero();
    }

    private long recordAt(VisitorKey visitor, String timestamp) {
        given(clock.instant()).willReturn(Instant.parse(timestamp));
        return interviewService.find(interviewId, null, visitor).viewCount();
    }

    private long count() {
        return jdbcTemplate.queryForObject("SELECT view_count FROM interviews WHERE id = ?", Long.class, interviewId);
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
