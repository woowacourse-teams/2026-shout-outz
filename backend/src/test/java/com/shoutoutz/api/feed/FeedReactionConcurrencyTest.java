package com.shoutoutz.api.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.feed.application.FeedReactionService;
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
class FeedReactionConcurrencyTest {

    private static final int THREAD_COUNT = 8;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FeedReactionService feedReactionService;

    private final List<Long> userIds = new ArrayList<>();
    private long feedId;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            userIds.add(insertUser());
        }
        feedId = jdbcTemplate.queryForObject("""
                INSERT INTO feeds(author_id, title, content, feed_type)
                VALUES (?, '동시성 검증', '본문', 'QUESTION') RETURNING id
                """, Long.class, userIds.getFirst());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM feeds WHERE id = ?", feedId);
        for (long userId : userIds) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
        userIds.clear();
    }

    @Test
    void 여러_사용자가_동시에_좋아요해도_좋아요_수가_누락되지_않는다() throws Exception {
        List<Callable<Long>> tasks = new ArrayList<>();
        for (long userId : userIds) {
            tasks.add(() -> {
                feedReactionService.add(feedId, userId, "LIKE");
                return 1L;
            });
        }

        run(tasks);

        assertThat(storedLikeCount()).isEqualTo(THREAD_COUNT);
        assertThat(actualLikeCount()).isEqualTo(THREAD_COUNT);
    }

    @Test
    void 같은_사용자가_동시에_여러_번_좋아요해도_한_번만_올린다() throws Exception {
        long userId = userIds.getFirst();
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> {
                feedReactionService.add(feedId, userId, "LIKE");
                return 1L;
            });
        }

        run(tasks);

        assertThat(storedLikeCount()).isEqualTo(1);
        assertThat(actualLikeCount()).isEqualTo(1);
    }

    @Test
    void 같은_좋아요를_동시에_여러_번_취소해도_한_번만_내린다() throws Exception {
        long userId = userIds.getFirst();
        feedReactionService.add(feedId, userId, "LIKE");
        feedReactionService.add(feedId, userIds.get(1), "LIKE");
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> {
                try {
                    feedReactionService.remove(feedId, userId, "LIKE");
                    return 1L;
                } catch (EntityNotFoundException exception) {
                    return 0L;
                }
            });
        }

        List<Long> results = run(tasks);

        assertThat(results.stream().mapToLong(Long::longValue).sum()).isEqualTo(1);
        assertThat(storedLikeCount()).isEqualTo(1);
        assertThat(actualLikeCount()).isEqualTo(1);
    }

    @Test
    void 좋아요와_취소가_동시에_섞여도_저장된_수가_실제_좋아요_수와_같다() throws Exception {
        int half = THREAD_COUNT / 2;
        for (int i = 0; i < half; i++) {
            feedReactionService.add(feedId, userIds.get(i), "LIKE");
        }
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < half; i++) {
            long removingUserId = userIds.get(i);
            long addingUserId = userIds.get(half + i);
            tasks.add(() -> {
                feedReactionService.remove(feedId, removingUserId, "LIKE");
                return 1L;
            });
            tasks.add(() -> {
                feedReactionService.add(feedId, addingUserId, "LIKE");
                return 1L;
            });
        }

        run(tasks);

        assertThat(storedLikeCount()).isEqualTo(half);
        assertThat(actualLikeCount()).isEqualTo(half);
    }

    private long storedLikeCount() {
        return jdbcTemplate.queryForObject("SELECT like_count FROM feeds WHERE id = ?", Long.class, feedId);
    }

    private long actualLikeCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_reactions WHERE feed_id = ? AND reaction_type = 'LIKE'",
                Long.class,
                feedId
        );
    }

    private long insertUser() {
        return jdbcTemplate.queryForObject("INSERT INTO users(handle) VALUES (?) RETURNING id", Long.class,
                "@f" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
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
