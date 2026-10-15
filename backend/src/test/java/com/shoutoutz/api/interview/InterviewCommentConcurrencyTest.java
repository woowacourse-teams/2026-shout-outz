package com.shoutoutz.api.interview;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.application.InterviewCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentCreateRequest;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
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

@SpringBootTest
@ActiveProfiles("test")
class InterviewCommentConcurrencyTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired InterviewCommentService comments;
    long userId;
    long interviewId;

    @BeforeEach
    void setUp() {
        userId = jdbc.queryForObject("INSERT INTO users(handle) VALUES (?) RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        jdbc.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '사용자')", userId);
        interviewId = jdbc.queryForObject("""
                INSERT INTO interviews(title, interviewee_id, author_id, status, published_at)
                VALUES ('동시성 검증', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, userId, userId);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM interviews WHERE id = ?", interviewId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void 동시에_등록해도_댓글수가_누락되지_않는다() throws Exception {
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> comments.create(interviewId, userId,
                    new InterviewCommentCreateRequest("동시 댓글", null)).id());
        }
        run(tasks);
        assertThat(jdbc.queryForObject("SELECT comment_count FROM interviews WHERE id = ?", Long.class, interviewId))
                .isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM interview_comments WHERE interview_id = ?", Long.class, interviewId))
                .isEqualTo(8);
    }

    @Test
    void 같은_댓글을_동시에_삭제해도_한번만_차감한다() throws Exception {
        long commentId = comments.create(interviewId, userId,
                new InterviewCommentCreateRequest("삭제 대상", null)).id();
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                try {
                    comments.delete(interviewId, commentId, userId);
                    return 1L;
                } catch (EntityNotFoundException exception) {
                    return 0L;
                }
            });
        }
        assertThat(run(tasks).stream().mapToLong(Long::longValue).sum()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT comment_count FROM interviews WHERE id = ?", Long.class, interviewId))
                .isZero();
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
