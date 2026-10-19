package com.shoutoutz.api.magazine;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.application.MagazineCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentCreateRequest;
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
class MagazineCommentConcurrencyTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MagazineCommentService magazineCommentService;

    private long userId;
    private long magazineId;

    @BeforeEach
    void setUp() {
        userId = jdbcTemplate.queryForObject("INSERT INTO users(handle) VALUES (?) RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        jdbcTemplate.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '사용자')", userId);
        magazineId = jdbcTemplate.queryForObject("""
                INSERT INTO magazines(title, featured_user_id, author_id, status, published_at)
                VALUES ('동시성 검증', ?, ?, 'PUBLISHED', now()) RETURNING id
                """, Long.class, userId, userId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM magazines WHERE id = ?", magazineId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void 동시에_등록해도_댓글수가_누락되지_않는다() throws Exception {
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> magazineCommentService.create(magazineId, userId,
                    new MagazineCommentCreateRequest("동시 댓글", null)).id());
        }
        run(tasks);
        assertThat(jdbcTemplate.queryForObject("SELECT comment_count FROM magazines WHERE id = ?", Long.class, magazineId))
                .isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM magazine_comments WHERE magazine_id = ?", Long.class, magazineId))
                .isEqualTo(8);
    }

    @Test
    void 같은_댓글을_동시에_삭제해도_한번만_차감한다() throws Exception {
        long commentId = magazineCommentService.create(magazineId, userId,
                new MagazineCommentCreateRequest("삭제 대상", null)).id();
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> {
                try {
                    magazineCommentService.delete(magazineId, commentId, userId);
                    return 1L;
                } catch (EntityNotFoundException exception) {
                    return 0L;
                }
            });
        }
        assertThat(run(tasks).stream().mapToLong(Long::longValue).sum()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT comment_count FROM magazines WHERE id = ?", Long.class, magazineId))
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
