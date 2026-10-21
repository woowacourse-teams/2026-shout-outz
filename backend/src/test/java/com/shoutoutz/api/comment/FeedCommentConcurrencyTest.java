package com.shoutoutz.api.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.comment.application.FeedCommentService;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentUpdateRequest;
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
class FeedCommentConcurrencyTest {

    private static final int THREAD_COUNT = 8;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FeedCommentService feedCommentService;

    private long userId;
    private long feedId;

    @BeforeEach
    void setUp() {
        userId = jdbcTemplate.queryForObject("INSERT INTO users(handle) VALUES (?) RETURNING id", Long.class,
                "@c" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        jdbcTemplate.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '사용자')", userId);
        feedId = jdbcTemplate.queryForObject("""
                INSERT INTO feeds(author_id, title, content, feed_type)
                VALUES (?, '동시성 검증', '본문', 'QUESTION') RETURNING id
                """, Long.class, userId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM feeds WHERE id = ?", feedId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void 동시에_댓글을_작성해도_댓글_수가_누락되지_않는다() throws Exception {
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> feedCommentService.create(feedId, userId,
                    new FeedCommentCreateRequest("동시 댓글", null)).id());
        }

        run(tasks);

        assertThat(storedCommentCount()).isEqualTo(THREAD_COUNT);
        assertThat(actualCommentCount()).isEqualTo(THREAD_COUNT);
    }

    @Test
    void 같은_댓글을_동시에_삭제해도_한_번만_내린다() throws Exception {
        long commentId = createComment("삭제 대상");
        createComment("남을 댓글");
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> {
                try {
                    feedCommentService.delete(feedId, commentId, userId);
                    return 1L;
                } catch (EntityNotFoundException exception) {
                    return 0L;
                }
            });
        }

        run(tasks);

        assertThat(isDeleted(commentId)).isTrue();
        assertThat(storedCommentCount()).isEqualTo(1);
        assertThat(actualCommentCount()).isEqualTo(1);
    }

    @Test
    void 수정과_삭제가_동시에_일어나도_지운_댓글이_살아나지_않는다() throws Exception {
        long commentId = createComment("원래 댓글");
        createComment("남을 댓글");
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT / 2; i++) {
            String content = "수정 " + i;
            tasks.add(() -> {
                try {
                    feedCommentService.update(feedId, commentId, userId, new FeedCommentUpdateRequest(content));
                    return 1L;
                } catch (EntityNotFoundException exception) {
                    return 0L;
                }
            });
            tasks.add(() -> {
                try {
                    feedCommentService.delete(feedId, commentId, userId);
                    return 1L;
                } catch (EntityNotFoundException exception) {
                    return 0L;
                }
            });
        }

        run(tasks);

        assertThat(isDeleted(commentId)).isTrue();
        assertThat(storedCommentCount()).isEqualTo(1);
        assertThat(actualCommentCount()).isEqualTo(1);
    }

    private long createComment(String content) {
        return feedCommentService.create(feedId, userId, new FeedCommentCreateRequest(content, null)).id();
    }

    private boolean isDeleted(long commentId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM feed_comments WHERE id = ?",
                Boolean.class,
                commentId
        ));
    }

    private long storedCommentCount() {
        return jdbcTemplate.queryForObject("SELECT comment_count FROM feeds WHERE id = ?", Long.class, feedId);
    }

    private long actualCommentCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_comments WHERE feed_id = ? AND deleted_at IS NULL",
                Long.class,
                feedId
        );
    }

    private List<Long> run(List<Callable<Long>> tasks) throws Exception {
        var executor = Executors.newFixedThreadPool(THREAD_COUNT);
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
